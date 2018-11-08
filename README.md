# LocalPrinting
Printing from Message Queue

Use this source code for send a document directly to printer from external server using [ActiveMQ](http://activemq.apache.org/) as Message Queue
![untitled document](https://user-images.githubusercontent.com/2333092/48182271-f681b180-e300-11e8-9a47-240449765b76.png)

Just run it like is showed:

<pre>
java -jar PrintService.jar host user pass queue printerName homefolder connectionInterval(optional)
</pre>

I use a simple docker container for [ActiveMQ](http://activemq.apache.org/) from https://hub.docker.com/r/rmohr/activemq/
 Run It:
<pre>
docker pull rmohr/activemq
</pre>

- Host: tcp://localhost:61616
- User: admin
- Password: admin
- Queue: printing
- Printer Name: LX300
- Home Folder: /tmp
- Connection Interval: 10000


Note: Server / Producer is not here!!! You can implement like is showed from official ActiveMQ for java: http://activemq.apache.org/hello-world.html
