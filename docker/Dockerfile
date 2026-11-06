# ---------------------------------------------------------------------------
# Stage 1: build. Gradle, its ~/.gradle cache and the source tree stay in this
# stage and are NOT copied into the runtime image.
# ---------------------------------------------------------------------------
FROM amazoncorretto:21-al2023 AS build

RUN dnf -y update \
 && dnf -y install --setopt=install_weak_deps=False wget unzip tar gzip findutils python3.12 python3.12-pip \
 && dnf clean all

WORKDIR /build
COPY . /build

# Gradle comes from the project wrapper (checksum-verified via gradle-wrapper.properties)
RUN ./gradlew shadowJar --no-daemon

# Cumulus Message Adapter (verify the download before using it)
ARG CMA_VERSION=v2.0.6
ARG CMA_SHA256=41a784d0db0293efc1efa366d1cdf4c9855e870d4ab1de8278f46cdfc882b271
RUN wget -q https://github.com/nasa/cumulus-message-adapter/releases/download/${CMA_VERSION}/cumulus-message-adapter.zip \
 && echo "${CMA_SHA256}  cumulus-message-adapter.zip" | sha256sum -c - \
 && mkdir /cumulus-message-adapter \
 && unzip -q -d /cumulus-message-adapter cumulus-message-adapter.zip

# The CMA release vendors urllib3 2.7.0 (vulnerable, fixed in 2.8.0): replace it.
# Remove the old package and its dist-info first so no 2.7.0 metadata is left behind.
ARG URLLIB3_VERSION=2.8.0
RUN rm -rf /cumulus-message-adapter/urllib3 /cumulus-message-adapter/urllib3-*.dist-info \
 && python3.12 -m pip install --no-cache-dir --no-deps --target /cumulus-message-adapter "urllib3==${URLLIB3_VERSION}"

# ---------------------------------------------------------------------------
# Stage 2: runtime. Only the jar, the CMA and what they need to run.
# ---------------------------------------------------------------------------
FROM amazoncorretto:21-al2023

# AL2023 uses dnf
RUN dnf -y update \
 && dnf -y install --setopt=install_weak_deps=False shadow-utils findutils python3.12 \
 && dnf clean all

# Make python3 resolve to Python 3.12 for shell/runtime use
# (do not remove system python packages)
RUN ln -sf /usr/bin/python3.12 /usr/local/bin/python3

RUN groupadd dockergroup \
 && useradd -m -g dockergroup dockeruser

WORKDIR /home/dockeruser

# Keep these paths: terraform/forge_fargate.tf runs
#   java -jar /home/dockeruser/build/libs/footprint.jar
COPY --from=build --chown=dockeruser:dockergroup /build/build/libs/footprint.jar /home/dockeruser/build/libs/footprint.jar
COPY --from=build --chown=dockeruser:dockergroup /cumulus-message-adapter /home/dockeruser/cumulus-message-adapter

ENV CUMULUS_MESSAGE_ADAPTER_DIR=/home/dockeruser/cumulus-message-adapter

USER dockeruser

ENTRYPOINT []
