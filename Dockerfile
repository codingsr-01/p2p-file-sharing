# One image with both programs: the default command starts a peer,
# docker-compose overrides it for the index server.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY src/ src/
RUN mkdir classes \
 && javac --release 17 -encoding UTF-8 -d classes $(find src -name '*.java') \
 && jar cfe IndexServer.jar NodoIndexServer.IndexServer -C classes . \
 && jar cfe PeerClientServer.jar NodoPeer.PeerClientServer -C classes .

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/IndexServer.jar /src/PeerClientServer.jar /app/
# a peer shares the folder /app/data/peer_<ID>, where <ID> is typed at start-up
CMD ["java", "-jar", "PeerClientServer.jar", "/app/data/peer_"]
