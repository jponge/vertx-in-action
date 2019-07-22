<template>
  <div>
    <div class="alert alert-danger" role="alert" v-if="alertMessage.length > 0">
      {{ alertMessage }}
    </div>
    <form v-on:submit="submit">
      <div class="form-group">
        <label for="username">User name</label>
        <input type="username" class="form-control" id="username" placeholder="somebody123" v-model="username">
      </div>
      <div class="form-group">
        <label for="email">Email</label>
        <input type="email" class="form-control" id="email" placeholder="foo@mail.me" v-model="email">
      </div>
      <div class="form-group">
        <label for="deviceId">Device identifier</label>
        <input type="deviceId" class="form-control" id="deviceId" placeholder="a1b2c3" v-model="deviceId">
      </div>
      <div class="form-group">
        <label for="password">Password</label>
        <input type="password" class="form-control" id="password" placeholder="abc123" v-model="password">
      </div>
      <div class="form-group">
        <label for="city">City</label>
        <input type="city" class="form-control" id="city" placeholder="Lyon" v-model="city">
      </div>
      <div class="form-check">
        <input class="form-check-input" type="checkbox" id="makePublic" v-model="makePublic">
        <label class="form-check-label" for="makePublic">
          I want to appear in public rankings
        </label>
      </div>
      <div class="form-group">
        <button type="submit" class="btn btn-primary">Submit</button>
      </div>
    </form>
  </div>
</template>

<script>
  import axios from 'axios'

  export default {
    data() {
      return {
        username: '',
        email: '',
        deviceId: '',
        city: '',
        password: '',
        makePublic: true,
        alertMessage: ''
      }
    },
    methods: {
      submit: function () {
        const payload = {
          username: this.username,
          email: this.email,
          deviceId: this.deviceId,
          password: this.password,
          city: this.city,
          makePublic: this.makePublic
        }
        axios
          .post("http://localhost:4000/api/v1/register", payload)
          .then(() => this.$router.push('/'))
          .catch(err => this.alertMessage = err.message)
      }
    }
  }
</script>
