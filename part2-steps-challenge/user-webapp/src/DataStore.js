export default {

  setToken(value) {
    localStorage.setItem("token", value)
  },

  token() {
    return localStorage.getItem("token")
  },

  hasToken() {
    return localStorage.getItem("token") !== null
  },

  setUsername(value) {
    localStorage.setItem("username", value)
  },

  username() {
    return localStorage.getItem("username")
  },

  setCity(value) {
    localStorage.setItem("city", value)
  },

  city() {
    return localStorage.getItem("city")
  },

  setDeviceId(value) {
    localStorage.setItem("deviceId", value)
  },

  deviceId() {
    return localStorage.getItem("deviceId")
  },

  setEmail(value) {
    localStorage.setItem("email", value)
  },

  email() {
    return localStorage.getItem("email")
  },

  setMakePublic(value) {
    localStorage.setItem("makePublic", value)
  },

  makePublic() {
    return localStorage.getItem("makePublic") === "true"
  },

  reset() {
    localStorage.clear()
  }
}
