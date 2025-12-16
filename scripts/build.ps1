# Build (Tageando para o registry remoto)
docker build -t localhost:5000/bet-backend:0.0.3 .

# Upload
docker push localhost:5000/bet-backend:0.0.3