# akka-fileupload-interview
# Description at /task

**Prerequisite**
Docker (brew install docker)
sbt (brew install sbt)
httpie (brew install httpie) - for test

**Docker Setup**

`sbt docker:publishLocal`

`docker run -p 8080:8080 akka-fileupload-interview:0.1`

**HealthCheck**

`http :8080/ping` - answer pong


**Testing Version v1**

Future Based Implementation

`http POST :8080/v1/images/upload <<< '{"urls": ["https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg","https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"]}'`

**Testing  Version v2**

Stream Pipeline Based Implementation

`http POST :8080/v2/images/upload <<< '{"urls": ["https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg","https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"]}'`
