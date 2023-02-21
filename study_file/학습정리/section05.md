# 싱글톤 컨테이너
## 웹 애플리케이션과 싱글톤
> 싱글톤: 객체 인스턴스가 현재 JVM 안에 하나만 존재하는 상태

### 웹 애플리케이션과 싱글톤
- 웹 애플리케이션은 보통 여러 고객이 동시에 요청을 한다.
- 고객이 요청할 때마다 객체를 생성하면 문제가 생긴다.    

test/../singleton/SingletonTest.java
```java
public class SingletonTest {

    @Test
    @DisplayName("스프링 없는 순수한 DI 컨테이너")
    void pureContainer() {
        AppConfig appConfig = new AppConfig();
        // 1. 조회: 호출할 때 마다 객체를 생성
        MemberService memberService1 = appConfig.memberService();

        // 1. 조회: 호출할 때 마다 객체를 생성
        MemberService memberService2 = appConfig.memberService();

        // 참조값이 다른 것을 확인
        System.out.println("memberService1 = " + memberService1);
        System.out.println("memberService2 = " + memberService2);
        
        // memberService != memberService2
        Assertions.assertThat(memberService1).isNotSameAs(memberService2);
    }
}
```
- 고객 트래픽이 초당 100이 나오면 초당 100개 객체가 생성되고 소멸된다
  - __메모리 낭비가 심하다!__
- 해당 객체가 딱 1개만 생성되고, 공유하도록 설계한다 -> __싱글톤__

## 싱글톤 컨테이너
- 클래스의 인스턴스가 딱 1개만 생성되는 것을 보장하는 디자인 패턴
- 객체 인스턴스를 2개 이상 생성하지 못하도록 막아야 한다.
  - private 생성자를 사용해서 외부에서 임의로 new 키워드를 사용하지 못하도록 막아야 한다.    
- static 객체로 만들어서 공유될 수 있도록 만든다.    

test/../singleton/SingletonService.java
```java
package hello.core.singleton;

public class SingletonService {

    private static final SingletonService instance = new SingletonService();

    // 조회 시 사용
    public static SingletonService getInstance() {
        return instance;
    }

    // 외부에서 new로 생성할 수 없다.
    private SingletonService() {
    }

    public void logic() {
        System.out.println("싱글톤 객체 로직 호출");
    }
}
```
- 인스턴스의 참조 확인은 getInstance()로만 가능하다.
  - 항상 같은 인스턴스를 반환한다.

```java
import static org.assertj.core.api.Assertions.*;

public class SingletonTest {

    @Test
    @DisplayName("싱글톤 패턴을 적용한 객체 사용")
    void singletonServiceTest() {
        SingletonService singletonService1 = SingletonService.getInstance();
        SingletonService singletonService2 = SingletonService.getInstance();

        System.out.println("singletonService1 = " + singletonService1);
        System.out.println("singletonService2 = " + singletonService2);

        assertThat(singletonService1).isSameAs(singletonService2);
    }
}
```
- same : == , 인스턴스 참조값 비교
- equal : 인스턴스 내용 비교

__스프링 컨테이너를 쓰면 객체를 기본적으로 싱글톤으로 만들어서 사용할 수 있게 만든다.__

### 싱글톤 패턴 문제점
- 싱글톤 패턴을 구현하는 코드 자체가 많이 들어간다.
- 의존관계상 클랑이언트가 구체 클래스에 의존한다. -> DIP, OCP 위반
- 테스트하기 어렵다.
- 내부 속성을 변경하거나 초기화하기 어렵다.
- private 생성자로 자식 클래스를 만들기 어렵다.
- 유연성 떨어짐!

## 싱글톤 컨테이너
스프링 컨테이너는 싱글톤 패턴의 문제점을 해결하면서, 객체 인스턴스를 싱글톤으로 관리한다.
- 스프링 컨테이너는 싱글톤 패턴을 적용하지 않아도, 객체 인스턴스를 싱글톤으로 관리한다.
- 스프링 컨테이너는 싱글톤 컨테이너 역할을 한다.
  - 싱글톤 객체를 생성하고 관리하는 기능을 싱글톤 레지스트리라 한다.
- 싱글톤 패턴을 위한 지저분한 코드가 들어가지 않아도 된다.
- DIP, OCP, 테스트, private 생성자로부터 자유롭게 싱글톤을 사용할 수 있다.

```java
public class SingletonTest {

    @Test
    @DisplayName("스프링 컨테이너와 싱글톤")
    void springContainer() {
//        AppConfig appConfig = new AppConfig();
        ApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);
        MemberService memberService1 = ac.getBean("memberService", MemberService.class);
        MemberService memberService2 = ac.getBean("memberService", MemberService.class);
        // 참조값이 다른 것을 확인
        System.out.println("memberService1 = " + memberService1);
        System.out.println("memberService2 = " + memberService2);

        // memberService != memberService2
        assertThat(memberService1).isSameAs(memberService2);
    }
}
```
- 이미 객체를 만들어서 고객의 요청이 오면 공유한다.
- 스프링은 요청할 때마다 새로운 객체를 생성해서 반환하는 기능도 제공한다.
  - 빈 스코프

## 싱글톤 방식의 주의점 <- 중요!
- 객체 인스턴스를 하나만 생성해서 공유하는 싱글톤 방식은 여러 클라이언트가 하나의 객체 인스턴스를 공유하기 때문에 __무상태(stateless)__ 로 설계해야 한다.
- 특정 클라이언트에 의존적인 필드가 있으면 안된다.
- 특정 클라이언트가 값을 변경할 수 있는 필드가 있으면 안된다.
- 가급적 읽기만 가능해야 한다.
- 필드 대신에 자바에서 공유되지 않는, 지역변수, 파라미터, ThreadLocal 등을 사용해야 한다.
- 스프링 빈의 필드에 공유 값을 설정하면 정말 큰 장애가 발생할 수 있다!
- 돈과 관련되면 진짜진짜진짜 큰 장애다.

## @Configuration과 싱글톤
- AppConfig의 코드를 보면 결과적으로 각각 다른 2개의 MemoryMemberService를 생성해서 싱글톤을 깨는 것처럼 보인다.

```java
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void join(Member member) {
        memberRepository.save(member);
    }

    @Override
    public Member findMember(Long memberId) {
        return memberRepository.findById(memberId);
    }

    // 테스트 용도
    public MemberRepository getMemberRepository() {
        return memberRepository;
    }
}
```

```java
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository;
    private final DiscountPolicy discountPolicy;

    public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
        this.memberRepository = memberRepository;
        this.discountPolicy = discountPolicy;
    }

    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int discountPrice = discountPolicy.discount(member, itemPrice);

        return new Order(memberId, itemName, itemPrice, discountPrice);
    }

    // 테스트 용도
    public MemberRepository getMemberRepository() {
        return memberRepository;
    }
}
```

test/../singleton/ConfigurationSingletonTest.java
```java
public class ConfigurationSingletonTest {

    @Test
    void configurationTest() {
        ApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);
        MemberServiceImpl memberService = ac.getBean("memberService", MemberServiceImpl.class);
        OrderServiceImpl orderService = ac.getBean("orderService", OrderServiceImpl.class);
        MemberRepository memberRepository = ac.getBean("memberRepository", MemberRepository.class);


        MemberRepository memberRepository1 = memberService.getMemberRepository();
        MemberRepository memberRepository2 = orderService.getMemberRepository();

        System.out.println("memberService -> memberRepository1 = " + memberRepository1);
        System.out.println("orderService -> memberRepository2 = " + memberRepository2);
        System.out.println("memberRepository = " + memberRepository);

        assertThat(memberService.getMemberRepository()).isSameAs(memberRepository);
        assertThat(orderService.getMemberRepository()).isSameAs(memberRepository);
    }
}
```
- 같은 인스턴스로 조회된다.
- memberRepository가 3번 호출되는 것을 기대했지만 1번만 호출된다.
  - 왜?

## @Configuration과 바이트코드 조작의 마법
- 스프링 컨테이너는 싱글톤 레지스트리이므로 스프링빈이 싱글톤이 되도록 보장해야 한다.
- 스프링이 자바 코드를 어떻게 하기는 어렵다.
- 그러나 메서드가 1번만 호출된다?
- 왜?
- @Configuration 에 비밀이 있다.

```java
public class ConfigurationSingletonTest {

    @Test
    void configurationDeep() {
        ApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);
        AppConfig bean = ac.getBean(AppConfig.class);

        System.out.println("bean = " + bean.getClass());
    }
}
```
__예상__
```
class hello.core.AppoConfig
```

__실제 출력__
```
bean = class hello.core.AppConfig$$EnhancerBySpringCGLIB$$993824f4
```

__이유__    
- 내가 만든 클래스가 아니라 스프링이 CGLIB라는 바이트코드 조작 라이브러리를 사용해서 AppConfig 클래스를 상속받은 임의의 다른 클래스를 만들고, 그 다른 클래스를 스프링 빈으로 등록한 것이다.
- 이 임의의 다른 클래스가 싱글톤이 보장되도록 해준다.
- @Bean이 붙은 메서드마다 이미 스프링 빈이 존재하면 존재하는 빈을 반환하고, 스프링 빈이 없으면 생성해서 스프링 빈으로 등록하고 반환하는 코드가 동적으로 만들어진다.

### @Configuration은 빼고 @Bean만 사용하면?
- 순수한 AppConfig가 호출된다.
- memberRepository가 3번 호출된다. -> 싱글톤 깨짐!
- 스프링 설정 정보는 항상 @Configuration을 사용하자!