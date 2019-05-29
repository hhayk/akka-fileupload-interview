# robo-voice-interview-task
# Description at /task

**Test via httpie (brew install httpie)**

**Version v1**
Future Based Implementation
`http POST http://localhost:8080/v1/images/upload <<< '{"urls": ["https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg","https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"]}'`

**Version v2**
Stream Graph Based Implementation
`http POST http://localhost:8080/v2/images/upload <<< '{"urls": ["https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg","https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"]}'`