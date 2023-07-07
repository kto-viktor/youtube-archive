gradle bootJar
docker build -t ugpt/yt_archiver:backend .
docker push ugpt/yt_archiver:backend