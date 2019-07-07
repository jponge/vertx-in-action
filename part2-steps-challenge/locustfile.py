from locust import *
from locust.exception import StopLocust
from random import *
from itertools import *
from datetime import datetime
import os
import json
import random

ID = datetime.now().strftime("%Y%m%d-%H-%M-%S")

FAST_MODE = os.getenv("FAST_TENK", 0) is not 0

def seq():
  n = 0
  while True:
    yield n
    n = n + 1

SEQ = seq()

def cities():
  names = [
    "Tassin La Demi Lune",
    "Lyon",
    "Sydney",
    "Aubiere",
    "Clermont-Ferrand",
    "Nevers",
    "Garchizy"
  ]
  return cycle(names)

CITIES = cities()

class UserWithDevice(TaskSet):

  def on_start(self):
    n = next(SEQ)
    self.username = "locust-user-%s-%s" % (ID, n)
    self.password = "abc_123!%s" % n
    self.email = "locust-user-%s-%s@mail.tld" % (ID, n)
    self.deviceId = "podometer-%s-%s-%s" % (n, n*2, ID)
    self.city = next(CITIES)
    self.makePublic = (n % 2) is 0
    self.deviceSync = 0
    self.register()
    self.fetch_token()

  def register(self):
    data = json.dumps({
      "username": self.username,
      "password": self.password,
      "email": self.email,
      "deviceId": self.deviceId,
      "city": self.city,
      "makePublic": self.makePublic
    })
    headers = {"Content-Type": "application/json"}
    with self.client.post("http://localhost:4000/api/v1/register", headers=headers, data=data, name="Register", catch_response=True) as response:
      if response.status_code != 200:
        self.interrupt()
        response.failure("Registration failed with data %s" % data)
        raise StopLocust()

  def fetch_token(self):
    data = json.dumps({
      "username": self.username,
      "password": self.password
    })
    headers = {"Content-Type": "application/json"}
    response = self.client.post("http://localhost:4000/api/v1/token", headers=headers, data=data, name="Fetch token")
    self.token = response.text

  @task(4)
  def send_steps(self):
    min_delta = 100 if FAST_MODE else 0
    max_delta = 1000 if FAST_MODE else 100
    self.deviceSync = self.deviceSync + 1
    data = json.dumps({
      "deviceId": self.deviceId,
      "deviceSync": self.deviceSync,
      "stepsCount": random.randint(min_delta, max_delta)
    })
    headers = {"Content-Type": "application/json"}
    self.client.post("http://localhost:3002/ingest", headers=headers, data=data, name="Steps update")

  @task(1)
  def my_profile_data(self):
    headers = {"Authorization": "Bearer %s" % self.token}
    self.client.get("http://localhost:4000/api/v1/%s" % self.username, headers=headers, name="Fetch profile data")

  @task(1)
  def how_many_total_steps(self):
    headers = {"Authorization": "Bearer %s" % self.token}
    with self.client.get("http://localhost:4000/api/v1/%s/total" % self.username, headers=headers, name="Fetch total steps", catch_response=True) as response:
      if response.status_code in (200, 404):
        response.success()

  @task(1)
  def how_many_steps_today(self):
    now = datetime.now()
    args = (self.username, now.year, now.month, now.day)
    headers = {"Authorization": "Bearer %s" % self.token}
    with self.client.get("http://localhost:4000/api/v1/%s/%s/%s/%s" % args, headers=headers, name="Fetch today total steps", catch_response=True) as response:
      if response.status_code in (200, 404):
        response.success()

class UserWithDeviceLocust(HttpLocust):
  task_set = UserWithDevice
  host = "localhost"
  min_wait = 10000 if not FAST_MODE else 500
  max_wait = 30000 if not FAST_MODE else 1000
