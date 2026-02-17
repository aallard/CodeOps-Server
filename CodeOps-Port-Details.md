## Platform Detail — CODEOPS

### Backend (Spring Boot) — Port: 8095

| Service | Port | Project |
|---------|------|---------|
| CodeOps Server | 8095 | `CodeOps-Server` |

### Frontend

| Service | Port | Project | Notes |
|---------|------|---------|-------|
| CodeOps Client (web) | 3200 | `CodeOps-Client` | Flutter web mode |
| CodeOps Client (native) | — | `CodeOps-Client` | macOS desktop app |

### Dedicated Infrastructure (CodeOps-Server/docker-compose.yml)

| Service | Host Port | Container Port |
|---------|-----------|----------------|
| PostgreSQL | 5434 | 5432 |
| Redis | 6380 | 6379 |
| Zookeeper | 2182 | 2181 |
| Kafka (external) | 9094 | 9092 |
| Kafka (internal) | — | 29092 |