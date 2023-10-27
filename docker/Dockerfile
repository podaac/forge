#FROM adoptopenjdk/openjdk11:alpine
FROM amazoncorretto:11-alpine-jdk

#RUN apk update
#RUN apk add --upgrade apk-tools
#RUN apk add gradle

RUN apk add wget
RUN apk add unzip
#RUN apk add python3==3.6.9-r3

#RUN adduser dockeruser --disabled-password

RUN addgroup -S dockergroup && adduser -S dockeruser -G dockergroup

WORKDIR /home/dockeruser
COPY . /home/dockeruser

# Download and install Gradle
RUN wget https://services.gradle.org/distributions/gradle-6.3-bin.zip && \
    unzip gradle-6.3-bin.zip && \
    rm gradle-6.3-bin.zip

# Add Gradle binaries to the PATH
ENV PATH="/home/dockeruser/gradle-6.3/bin:${PATH}"

# Display Gradle version to verify the installation
RUN gradle --version

RUN chown dockeruser:dockergroup -R /home/dockeruser

USER dockeruser

RUN ./gradlew shadowJar

RUN wget https://github.com/nasa/cumulus-message-adapter/releases/download/v2.0.3/cumulus-message-adapter.zip
RUN mkdir cumulus-message-adapter
RUN unzip -d cumulus-message-adapter cumulus-message-adapter.zip

ENV CUMULUS_MESSAGE_ADAPTER_DIR=/home/dockeruser/cumulus-message-adapter

ENTRYPOINT []
