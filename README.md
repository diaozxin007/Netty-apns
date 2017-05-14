#This a java apns client.

#Test:
```java
public class apnsTest {

    private ApnsService apnsService;
    @Before
    public void init(){
        ApnsConfig apnsConfig = new ApnsConfig();
        //apnsConfig.setDevEnv(PushConfig.apnsProduction);

        apnsConfig.setKeyStorePath("APNS.p12");
        apnsConfig.setPassword("");
        apnsConfig.setDevEnv(true);
        apnsService = new NettyApnsService(apnsConfig);

    }


    @Test
    public void sendNotification() throws Exception {

        String token = "sFsDgPKihHrfM02eDNsXxcBd0pJ0KQYn6H60IUYZWFM=";
        token = Hex.encodeHexString(Base64.decodeBase64(token));

        Payload payload = new Payload();
        payload.setAlert("How are you?");
        payload.setBadge(Integer.valueOf(1));
        apnsService.sendNotification(token, payload);
        Thread.sleep(800000);
    }
    }


```
