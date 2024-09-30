FROM alpine AS build
RUN apk update \
 && apk add openjdk21-jre
RUN apk add bash clang-extra-tools git
ADD . /LightOidc
WORKDIR /LightOidc
RUN VERSION=$(clang-format --version | sed -e "s/.* //g") \
 && sed -i "s|clangFormat(.*).style|clangFormat('$VERSION').style|g" build.gradle \
 && ./gradlew jar \
 && mv *app/build/libs/*.jar /lightoidc.jar

FROM alpine
RUN apk update \
 && apk add openjdk21-jre
COPY --from=build /lightoidc.jar /opt/lightoidc.jar
RUN adduser -S lightoidc \
 && mkdir /data /home/lightoidc/.config \
 && chown lightoidc /data /home/lightoidc/.config \
 && ln -s /data /home/lightoidc/.config/LightOIDC

USER lightoidc
ENTRYPOINT java -jar /opt/lightoidc.jar