<template>
  <div>
    <div class="alert alert-danger" role="alert" v-if="alertMessage.length > 0">
      {{ alertMessage }}
    </div>
    <form v-on:submit="login">
      <div class="form-group">
        <label for="username">User name</label>
        <input type="username" class="form-control" id="username" placeholder="somebody123" v-model="username">
      </div>
      <div class="form-group">
        <label for="password">Password</label>
        <input type="password" class="form-control" id="password" placeholder="abc123" v-model="password">
      </div>
      <button type="submit" class="btn btn-primary">Submit</button>
    </form>
    <div>
      <p>...or <router-link to="/register">register</router-link></p>
    </div>
  </div>
</template>

<script>
  import DataStore from '../DataStore'
  import axios from 'axios'

  export default {
    data() {
      return {
        username: '',
        password: '',
        alertMessage: ''
      }
    },
    methods: {
      login: function () {
        if (this.username.length === 0 || this.password.length === 0) {
          return
        }
        axios
          .post("http://localhost:4000/api/v1/token", {
            username: this.username,
            password: this.password
          })
          .then(response => {
            DataStore.setToken(response.data)
            DataStore.setUsername(this.username)
            this.$router.push({name: 'home'})
          })
          .catch(err => this.alertMessage = err.message)
      }
    }
  }
</script>
