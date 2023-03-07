# 빈 생명주기 콜백
## 빈 생명주기 콜백 시작
데이터베이스 커넥션 풀이나, 네트워크 소켓처럼 애플리케이션 시작 시점에 필요한 연결을 미리 해두고, 
애플리케이션 종료 시점에 연결을 모두 종료하는 작업을 진행하려면, 객체의 초기화와 종료 작업이 필요하다.

<br/>

test/../lifecycle/NetworkClient.java
```java
public class NetworkClient {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
        connect();
        call("초기화 연결 메시지");
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    // 서비스 종료시 호출
    public void disconnect() {
        System.out.println("close " + url);
    }
}
```
lifecycle/BeanLifeCycleTest.java
```java
public class BeanLifeCycleTest {

    @Test
    public void lifeCycleTest() {
        ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(LifeCycleConfig.class);
        NetworkClient client = ac.getBean(NetworkClient.class);
        ac.close();
    }

    @Configuration
    static class LifeCycleConfig {
        @Bean
        public NetworkClient networkClient() {
            NetworkClient networkClient = new NetworkClient();
            networkClient.setUrl("http://hello-spring.dev");
            return networkClient;
        }
    }
}
```
- 이때 생성자 부분을 보면 url 정보 없이 connect가 호출되므로 url에 null이 들어간다.
- 객체를 생성한 다음에 외부에서 수정자 주입을 통해서 setUrl()이 호출되어야 url이 존재하게 된다.

### 스프링 빈의 이벤트 라이프사이클(싱글톤)
스프링 컨테이너 생성 -> 스프링 빈 생성 -> 의존관계 주입 -> 초기화 콜백(초기화 시점 알려줌)-> 사용 -> 소멸 전 콜백(스프링 컨테이너가 종료되기 직전) -> 스프링 종료

> 객체의 생성과 초기화를 분리하자:    
> 생성자는 필수 정보(파라미터)로 받고, 메모리를 할당해서 객체를 생성하는 책임을 가진다.    
> 초기화는 이렇게 생성된 값들을 활용해서 외부 커넥션을 연결하는 등 무거운 동작을 수행한다.    
> 이 두 부분을 명확하게 나누는 것이 유지보수 관점에서 좋다.

### 스프링의 빈 생명주기 콜백 지원 방법
1. 인터페이스(InitializingBean, DisposableBean)
2. 설정 정보에 초기화 메서드, 종료 메서드 지정
3. @PostConstruct, @PreDestroy 애노테이션 지원

## 인터페이스(InitializingBean, DisposableBean)
```java
public class NetworkClient implements InitializingBean, DisposableBean {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    // 서비스 종료시 호출
    public void disconnect() {
        System.out.println("close " + url);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 의존관계 주입이 끝나면 호출
        connect();
        call("초기화 연결 메시지");
    }

    @Override
    public void destroy() throws Exception {
        // 빈이 종료될 때 호출
        disconnect();
    }
}
```
- 생성자 호출 후 의존관계 주입이 끝나면 url이 세팅된 것을 볼 수 있다.

### 초기화, 소멸 인터페이스의 단점
- 스프링 전용 인터페이스이므로, 스프링에 의존할 수밖에 없다.
  - 지금은 더 나은 방법들이 있어서 거의 사용하지 않는다.
- 초기화, 소멸 메서드의 이름을 변경할 수 없다.
- 내가 코드를 고칠 수 없는 외부 라이브러리에 적용할 수 없다.

## 빈 등록 초기화, 소멸 메서드
빈을 등록하는 시점에 어떤 것이 초기화인지, 어떤 것이 소멸인지 알려줄 수 있다.
```java
public class NetworkClient {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    // 서비스 종료시 호출
    public void disconnect() {
        System.out.println("close " + url);
    }

    public void init() throws Exception {
        // 의존관계 주입이 끝나면 호출
        System.out.println("NetworkClient.init");
        connect();
        call("초기화 연결 메시지");
    }

    public void close() throws Exception {
        // 빈이 종료될 때 호출
        System.out.println("NetworkClient.close");
        disconnect();
    }
}
```
```java
public class BeanLifeCycleTest {

    @Test
    public void lifeCycleTest() {
        ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(LifeCycleConfig.class);
        NetworkClient client = ac.getBean(NetworkClient.class);
        ac.close();
    }

    @Configuration
    static class LifeCycleConfig {
        // 변경
        @Bean(initMethod = "init", destroyMethod = "close")
        public NetworkClient networkClient() {
            NetworkClient networkClient = new NetworkClient();
            networkClient.setUrl("http://hello-spring.dev");
            return networkClient;
        }
    }
}
```
- 스프링 빈이 스프링 코드에 의존하지 않는다.
- 메서드 이름을 자유롭게 줄 수 있다.
- 코드가 아니라 설정 정보를 사용하기 때문에 코드를 코칠 수 없는 외부 라이브러리에도 초기화, 종료 메서드를 적용할 수 있다.
- destroyMethod는 기본적으로 (inferred) 으로 등록되어 있다.
  - 이 추론 기능은 close, shutdown이라는 이름의 메서드를 자동으로 호출해준다.
  - 직접 스프링 빈으로 등록하면(@Bean) 종료 메서드는 따로 적어주지 않아도 잘 동작한다.

## 애노테이션 @PostConstruct, @PreDestroy 
결론적으로 이 방법을 사용하면 된다.
```java
public class BeanLifeCycleTest {

    @Test
    public void lifeCycleTest() {
        ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(LifeCycleConfig.class);
        NetworkClient client = ac.getBean(NetworkClient.class);
        ac.close();
    }

    @Configuration
    static class LifeCycleConfig {
        // 변경
        @Bean
        public NetworkClient networkClient() {
            NetworkClient networkClient = new NetworkClient();
            networkClient.setUrl("http://hello-spring.dev");
            return networkClient;
        }
    }
}
```
```java
package hello.core.lifecycle;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class NetworkClient {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    // 서비스 종료시 호출
    public void disconnect() {
        System.out.println("close " + url);
    }

    @PostConstruct
    public void init() throws Exception {
        // 의존관계 주입이 끝나면 호출
        System.out.println("NetworkClient.init");
        connect();
        call("초기화 연결 메시지");
    }

    @PreDestroy
    public void close() throws Exception {
        // 빈이 종료될 때 호출
        System.out.println("NetworkClient.close");
        disconnect();
    }
}
```
- javax.annotation.PostConstruct 이다. 
  - 스프링에 종속적인 기술이 아니라 다른 컨테이너에서도 동작한다.
  - 컴포넌트 스캔과 잘 어울린다.
  - 외부 라이브러리에는 적용하지 못한다. 그 때는 @Bean의 기능을 사용하자.
