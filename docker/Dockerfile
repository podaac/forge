FROM openjdk:11-jdk-alpine

RUN apk add gradle
RUN apk add python3==3.6.9-r3

#RUN adduser dockeruser --disabled-password

RUN addgroup -S dockergroup && adduser -S dockeruser -G dockergroup

WORKDIR /home/dockeruser
COPY . /home/dockeruser

RUN chown dockeruser:dockergroup -R /home/dockeruser

USER dockeruser

RUN ./gradlew shadowJar

RUN wget https://github.com/nasa/cumulus-message-adapter/releases/download/v2.0.3/cumulus-message-adapter.zip
RUN mkdir cumulus-message-adapter
RUN unzip -d cumulus-message-adapter cumulus-message-adapter.zip

ENV CUMULUS_MESSAGE_ADAPTER_DIR=/home/dockeruser/cumulus-message-adapter

ENTRYPOINT []
