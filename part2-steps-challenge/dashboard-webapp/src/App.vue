<template>
  <div id="app">
    <div class="row">
      <div class="col">
        <h4>
          <span class="badge badge-pill badge-dark">{{ throughput }}</span> device updates per second
        </h4>
      </div>
    </div>
  </div>
</template>

<script>
  import EventBus from 'vertx3-eventbus-client'

  const eventBus = new EventBus("/eventbus")
  eventBus.enableReconnect(true)

  export default {
    data() {
      return {
        throughput: 0
      }
    },
    mounted() {
      eventBus.onopen = () => {
        eventBus.registerHandler("client.updates.throughput", (err, message) => {
          this.throughput = message.body.throughput
        })
      }
    },
    methods: {}
  }
</script>
