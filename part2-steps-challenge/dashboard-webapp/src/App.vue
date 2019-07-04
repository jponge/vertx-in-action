<template>
  <div id="app">
    <div class="row mt-5">
      <div class="col">
        <h4>
          <span class="badge badge-pill badge-dark">{{ throughput }}</span> device updates per second
        </h4>
      </div>
    </div>
    <div class="row mt-5">
      <div class="col">
        <h4>Trends</h4>
        <table class="table table-sm table-hover">
          <thead>
          <tr>
            <th scope="col">City</th>
            <th scope="col">Steps</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in cityTrendRanking" v-bind:key="item.city">
            <td scope="row">{{ item.city }}</td>
            <td>+{{ item.stepsCount }}</td>
          </tr>
          </tbody>
        </table>
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
        throughput: 0,
        cityTrendData: {}
      }
    },
    mounted() {
      eventBus.onopen = () => {
        eventBus.registerHandler("client.updates.throughput", (err, message) => {
          this.throughput = message.body.throughput
        })
        eventBus.registerHandler("client.updates.city-trend", (err, message) => {
          this.$set(this.cityTrendData, message.body.city, message.body)
        })
      }
    },
    computed: {
      cityTrendRanking: function () {
        const values = Object.values(this.cityTrendData).slice(0)
        values.sort((a, b) => b.stepsCount - a.stepsCount)
        return values
      }
    },
  }
</script>
