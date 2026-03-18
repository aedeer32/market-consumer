job "market-consumer" {
  datacenters = ["dc1"]
  type = "service"

  group "consumer" {
    task "app" {
      driver = "docker"

      env {
        KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
        KAFKA_TOPIC             = "prices"
        KAFKA_GROUP_ID          = "market-consumer-group"
        POSTGRES_URL            = "jdbc:postgresql://localhost:5433/appdb"
        POSTGRES_USER           = "appuser"
        POSTGRES_PASSWORD       = "apppass"
      }

      config {
        image        = "market-consumer:latest"
        network_mode = "host"
      }

      resources {
        cpu    = 500
        memory = 512
      }
    }
  }
}