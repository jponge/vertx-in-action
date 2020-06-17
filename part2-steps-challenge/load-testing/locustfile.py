from locust import *
from random import *
from itertools import *
from datetime import datetime
import os
import json
import uuid
import random

cities = cycle([
  "Tassin La Demi Lune",
  "Lyon",
  "Sydney",
  "Aubiere",
  "Clermont-Ferrand",
  "Nevers",
  "Garchizy"
])

sequence = count()

class UserBehavior(TaskSet):

  def on_start(self):
    n = next(sequence)
    self.registered = False
    self.token = None
    self.username = f"user{n}-{str(uuid.uuid1())}"
    self.password = str(uuid.uuid4())
    self.email = f"user{n}@my.tld"
    self.deviceId = str(uuid.uuid1())
    self.city = next(cities)
    self.makePublic = (n % 2) == 0
    self.deviceSync = 0

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
    with self.client.post(":4000/api/v1/register", headers=headers, data=data, name="Register", catch_response=True) as response:
      if response.status_code in (200, 409):
        self.registered = True
        response.success()

  def fetch_token(self):
    data = json.dumps({
      "username": self.username,
      "password": self.password
    })
    headers = {"Content-Type": "application/json"}
    with self.client.post(":4000/api/v1/token", headers=headers, data=data, name="Fetch token", catch_response=True) as response:
      if response.status_code == 200:
        self.token = response.text
        response.success()

  def is_ready(self):
    if not self.registered:
      self.register()
    if self.token == None:
      self.fetch_token()
    return self.registered and (self.fetch_token != None)

  @task(80)
  def send_steps(self):
    if not self.is_ready():
      return
    self.deviceSync = self.deviceSync + 1
    data = json.dumps({
      "deviceId": self.deviceId,
      "deviceSync": self.deviceSync,
      "stepsCount": random.randint(0, 100)
    })
    headers = {"Content-Type": "application/json"}
    self.client.post(":3002/ingest", headers=headers, data=data, name="Steps update")

  @task(5)
  def my_profile_data(self):
    if not self.is_ready():
      return
    headers = {"Authorization": f"Bearer {self.token}"}
    self.client.get(f":4000/api/v1/{self.username}", headers=headers, name="Fetch profile data")

  @task(5)
  def how_many_total_steps(self):
    if not self.is_ready():
      return
    headers = {"Authorization": f"Bearer {self.token}"}
    with self.client.get(f":4000/api/v1/{self.username}/total", headers=headers, name="Fetch total steps", catch_response=True) as response:
      if response.status_code in (200, 404):
        response.success()

  @task(10)
  def how_many_steps_today(self):
    if not self.is_ready():
      return
    now = datetime.now()
    headers = {"Authorization": f"Bearer {self.token}"}
    with self.client.get(f":4000/api/v1/{self.username}/{now.year}/{now.month}/{now.day}", headers=headers, name="Fetch today total steps", catch_response=True) as response:
      if response.status_code in (200, 404):
        response.success()

class UserWithDevice(HttpUser):
  tasks = [UserBehavior]
  host = "http://localhost"
  wait_time = between(0.5, 2.0)
