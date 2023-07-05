FROM eclipse-temurin:17-jdk-focal
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN ["apt-get", "update"]
RUN ["apt-get", "install", "-y", "software-properties-common"]
RUN ["add-apt-repository", "ppa:tomtomtom/yt-dlp"]
#RUN ["echo", "deb https://ppa.launchpadcontent.net/tomtomtom/yt-dlp/ubuntu focal main", ">>", "/etc/apt/source.list"]
#RUN ["echo", "deb-src https://ppa.launchpadcontent.net/tomtomtom/yt-dlp/ubuntu focal main", ">>", "/etc/apt/source.list"]
RUN ["apt-get", "update"]
RUN ["apt-get", "install", "-y", "yt-dlp"]
RUN ["curl", "-OL", "https://launchpad.net/~tomtomtom/+archive/ubuntu/yt-dlp/+build/25643369/+files/yt-dlp_2023.03.04-1~ppa~focal_all.deb"]
RUN ["dpkg", "-i", "yt-dlp_2023.03.04-1~ppa~focal_all.deb"]
RUN ["apt-get", "install", "-y", "ffmpeg"]
ENTRYPOINT ["java","-jar","/app.jar"]