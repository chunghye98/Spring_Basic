# 스프링 핵심 원리 이해2 - 객체지향 원리 적용
## 새로운 할인 정책 개발
- 할인 정책을 변경하고 싶다!
- 이전 코드가 객체지향 설계 원칙을 잘 준수 했는지 살펴보자

hello/core/discount/RateDiscountPolicy.java
```java
public class RateDiscountPolicy implements DiscountPolicy {

    private int discountPercent = 10;
    
    @Override
    public int discount(Member member, int price) {
        if (member.getGrade() == Grade.VIP) {
            return price * discountPercent / 100;
        } else {
            return 0;
        }
    }
}
```

test/../RateDiscountPolicy.java
```java
class RateDiscountPolicyTest {

    RateDiscountPolicy discountPolicy = new RateDiscountPolicy();

    @Test
    @DisplayName("VIP는 10% 할인이 적용되어야 한다.")
    void vip_o() {
        //given
        Member member = new Member(1L, "memberVIP", Grade.VIP);
        //when
        int discount = discountPolicy.discount(member, 10000);
        //then
        Assertions.assertThat(discount).isEqualTo(1000);
    }

    @Test
    @DisplayName("VIP가 아니면 할인이 적용되지 않아야 한다.")
    void vip_x() {
        //given
        Member member = new Member(2L, "memberBasic", Grade.BASIC);
        //when
        int discount = discountPolicy.discount(member, 10000);
        //then
        Assertions.assertThat(discount).isEqualTo(0);
    }
}
```
- 한 메서드에 해당하는 성공/실패 테스트 코드를 만들어야 한다.

## 새로운 할인 정책 적용과 문제점
- 할인 정책을 변경하려면 클라이언트인 OrderServiceImpl 코드를 고쳐야 한다.
```java
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository = new MemoryMemberRepository();
//    private final DiscountPolicy discountPolicy = new FixDiscountPolicy();
    private final DiscountPolicy discountPolicy = new RateDiscountPolicy(); // 변경!

    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int discountPrice = discountPolicy.discount(member, itemPrice);

        return new Order(memberId, itemName, itemPrice, discountPrice);
    }
}
```

### 문제점
- 역할과 구현 분리 O
- 다형성도 활용하고, 인터페이스와 구현 객체 분리 O
- OCP, DIP 같은 객체지향 설계 원칙을 준수했다 X
  - 실제로 OrderServiceImpl은 DiscountPolicy 뿐만 아니라 FixDiscountPolicy에도 의존한다.
  - 추상과 구체에 둘 다 의존했다.
  - 지금 코드는 기능을 확장해서 변경하면 클라이언트 코드에 영향을 주므로 OCP를 위반한다.

### 해결 방법
DIP를 의존하지 않도록 인터페이스에만 의존하도록 의존 관계를 변경한다.
```java
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository = new MemoryMemberRepository();
    private DiscountPolicy discountPolicy; // 변경!

    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int discountPrice = discountPolicy.discount(member, itemPrice);

        return new Order(memberId, itemName, itemPrice, discountPrice);
    }
}
```
- 구현체가 없으므로 Null Pointer Exception이 발생한다.
- 이 문제를 해결하려면 OrderServiceImpl에 DiscountPolicy의 구현 객체를 __대신 생성하고 주입__ 해주어야 한다.

## 관심사의 분리
- 지금까지의 코드는 배우가 자기 연기도 하면서 다른 배우도 초빙하는 __다양한 책임__ 을 가지고 있었다.
- 배우가 본인의 역할인 배역을 수행하는 것에만 집중해야 한다.
- __공연 기획자__ 가 있어야 한다.

### AppConfig
애플리케이션 전체 동작 방식을 구성(config)하기 위해, 구현 객체를 생성하고 연결하는 책임을 가지는 별도의 설정 클래스

hello/core/AppConfig.java
```java
public class AppConfig {

    public MemberService memberService() {
        return new MemberServiceImpl(new MemoryMemberRepository()); // 생성자 주입
    }

    public OrderService orderService() {
        return new OrderServiceImpl(
                new MemoryMemberRepository(), 
                new FixDiscountPolicy());
    }
}
```
- AppConfig는 애플리케이션의 실제 동작에 필요한 __구현 객체를 생성__ 한다.
- AppConfig는 생성한 객체 인스턴스의 참조(레퍼런스)를 __생성자를 통해서 주입(연결)__ 해준다.

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
}
```
- 설계 변경으로 MemberServiceImpl은 MemberRepository 인터페이스에만 의존한다.
- MemberServiceImpl의 입장에서 생성자를 통해 어떤 구현 객체가 들어올지(주입될지)는 알 수 없다.
  - 어떤 구현 객체를 주입할지는 오직 외부(AppConfig)에서 결정된다.
- MemberServiceImpl은 이제부터 __의존관계에 대한 고민은 외부__ 에 맡기고 __실행에만 집중__ 하면 된다.
- __DIP 완성__: MemberServiceImpl은 추상에만 의존
- __관심사의 분리__: 객체를 생성하고 연결하는 역할과 실행하는 역할이 명확히 분리되었다.
- 클라이언트인 MemberServiceImpl에서 의존관계를 외부에서 주입해주는 것 같다고 해서 DI(Dependency Injection), 의존관계 주입 또는 의존성 주입이라 한다.

<br/>

OrderServiceImpl - 생성자 주입
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
}
```

### AppConfig 실행
MemberApp
```java
public class MemberApp {
    public static void main(String[] args) {
        AppConfig appConfig = new AppConfig();
        MemberService memberService = appConfig.memberService();

        Member member = new Member(1L, "memberA", Grade.VIP);
        memberService.join(member);

        Member findMember = memberService.findMember(1L);
        System.out.println("new member = " + member.getName());
        System.out.println("findMember = " + findMember.getName());
    }
}
```
OrderApp
```java
public class OrderApp {
    public static void main(String[] args) {
        AppConfig appConfig = new AppConfig();
        MemberService memberService = appConfig.memberService();
        OrderService orderService = appConfig.orderService();

        Long memberId = 1L;
        Member member = new Member(memberId, "memberA", Grade.VIP);
        memberService.join(member);

        Order order = orderService.createOrder(memberId, "itemA", 10000);

        System.out.println("order = " + order);
    }
}
```

MemberServiceTest
```java
class MemberServiceTest {

    MemberService memberService;

    @BeforeEach
    public void beforeEach() {
        AppConfig appConfig = new AppConfig();
        memberService = appConfig.memberService();
    }
}
```

OrderServiceImpl
```java
class OrderServiceTest {

    MemberService memberService;
    OrderService orderService;

    @BeforeEach
    public void beforeEach() {
        AppConfig appConfig = new AppConfig();
        memberService = appConfig.memberService();
        orderService = appConfig.orderService();
    }
}
```

## AppConfig 리팩터링
현재 AppConfig를 보면 __중복__ 이 있고, __역할__ 에 따른 __구현__ 이 잘 안보인다.
```java
public class AppConfig {

    public MemberService memberService() {
        return new MemberServiceImpl(memberRepository()); // 생성자 주입
    }

    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }

    public OrderService orderService() {
        return new OrderServiceImpl(
                memberRepository(),
                discountPolicy());
    }

    public DiscountPolicy discountPolicy() {
        return new FixDiscountPolicy();
    }
}
```
- AppConfig를 보면 역할과 구현 클래스가 한 눈에 들어온다. 애플리케이션 구성이 어떻게 되어있는지 빠르게 파악할 수 있다.

## 새로운 구조와 할인 정책 적용
- FixDiscountPolicy -> RateDiscountPolicy
- AppConfig만 변경하면 된다.
- 구성 영역만 영향을 받고, 사용 영역은 전혀 영향을 받지 않는다.

AppConfig
```java
public class AppConfig {

    public MemberService memberService() {
        return new MemberServiceImpl(memberRepository()); // 생성자 주입
    }

    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }

    public OrderService orderService() {
        return new OrderServiceImpl(
                memberRepository(),
                discountPolicy());
    }

    public DiscountPolicy discountPolicy() {
        return new RateDiscountPolicy(); // 변경!
    }
}
```
- 사용 영역에 있는 코드는 전혀 손댈 필요가 없어졌다.

## 좋은 객체 지향 설계의 5가지 원칙의 적용
### SRP 단일 책임 원칙
한 클래스는 하나의 책임만 가져야 한다.
- 관심사 분리
- 구현 객체를 생성하고 연결하는 책임 -> AppConfig
- 실행하는 책임 -> 클라이언트 객체

### DIP 의존관계 역전 원칙
프로그래머는 추상화에 의존해야지, 구체화에 의존하면 안된다.    
의존성 주입은 이 원칙을 따르는 방법 중 하나다.

### OCP 개방 폐쇄 원칙
소프트웨어 요소는 확장에는 열려 있으나 변경에는 닫혀 있어야 한다.
- 소프트웨어 요소를 새롭게 확장해도 사용 영역의 변경은 닫혀있다!

## IoC, DI, 그리고 컨테이너
### 제어의 역전 IoC(Inversion Of Control)
- 기존 프로그램은 클라이언트 구현 객체가 스스로 필요한 서버 구현 객체를 생성하고, 연결하고, 실행했다.
- AppConfig가 등장한 이후 프로그램의 제어 흐름은 AppConfig가 가져간다.
- 이와 같이 프로그램의 제어 흐름을 직접 제어하는 것이 아니라 외부에서 관리하는 것을 __제어의 역전(IoC)__ 라 한다.

> 프레임워크 vs 라이브러리: 
> - 내가 작성한 코드를 제어하고, 대신 실행하면 그것은 프레임워크다.
> - 내가 작성한 코드가 직접 제어의 흐름을 담당한다면 그것은 라이브러리다.

### 의존관계 주입 DI(Dependency Injection)
- 의존관계는 정적인 클래스 의존관계와 실행 시점에 결정되는 동적인 객체(인스턴스) 의존관계 둘을 분리해서 생각해야 한다.
  - 정적인 클래스 의존관계: 클래스가 사용하는 import 코드만 보고 의존관계를 쉽게 판단할 수 있다.
  - 동적인 객체 인스턴스 의존관계: 애플리케이션 실행 시점에 실제 생성된 객체 인스턴스의 참조가 연결된 의존 관계다.
- 애플리케이션 __실행 시점__ 에 외부에서 실제 구현 객체를 생성하고 클라이언트에 전달해서 클라이언트와 서버의 실제 의존관계가 연결 되는 것을 __의존관계 주입__ 이라 한다.

### IoC 컨테이너, DI 컨테이너
- AppConfig 처럼 객체를 생성하고 관리하면서 의존관계를 연결해 주는 것을 IoC 컨테이너 또는 __DI 컨테이너__ 라 한다.
- 어샘블러, 오브젝트 팩토리 등으로 불리기도 한다.

## 스프링으로 전환하기
AppConfig
```java
@Configuration
public class AppConfig {

    @Bean
    public MemberService memberService() {
        return new MemberServiceImpl(memberRepository()); // 생성자 주입
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }

    @Bean
    public OrderService orderService() {
        return new OrderServiceImpl(
                memberRepository(),
                discountPolicy());
    }

    @Bean
    public DiscountPolicy discountPolicy() {
        return new RateDiscountPolicy();
    }
}
```
- @Configuration을 붙여주고 각 메서드에 @Bean을 붙인다.
- 스프링 컨테이너에 스프링 빈으로 등록한다.

<br/>

MemberApp
```java
public class MemberApp {
    public static void main(String[] args) {
//        AppConfig appConfig = new AppConfig();
//        MemberService memberService = appConfig.memberService();

        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        MemberService memberService = applicationContext.getBean("memberService", MemberService.class);
        
        Member member = new Member(1L, "memberA", Grade.VIP);
        memberService.join(member);

        Member findMember = memberService.findMember(1L);
        System.out.println("new member = " + member.getName());
        System.out.println("findMember = " + findMember.getName());
    }
}
```
- 스프링 빈에 등록이 될 때 메서드명이 key, return 값이 value로 들어간다.
- 따라서 메서드명을 key로 value 값을 찾아 인스턴스를 생성한다.    

<br/>

OrderService
```java
public class OrderApp {
    public static void main(String[] args) {
//        AppConfig appConfig = new AppConfig();
//        MemberService memberService = appConfig.memberService();
//        OrderService orderService = appConfig.orderService();

        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        MemberService memberService = applicationContext.getBean("memberService", MemberService.class);
        OrderService orderService = applicationContext.getBean("orderService", OrderService.class);

        Long memberId = 1L;
        Member member = new Member(memberId, "memberA", Grade.VIP);
        memberService.join(member);

        Order order = orderService.createOrder(memberId, "itemA", 10000);

        System.out.println("order = " + order);
    }
}
```
- ApplicationContext를 스프링 컨테이너라 한다.
- 이제는 스프링 컨테이너를 통해서 DI를 한다.
- 스프링 컨테이너는 @Configuration이 붙은 AppConfig를 설정(구성) 정보로 사용한다.
  - @Bean이라 적인 메서드를 모두 호출해서 반환된 객체를 스프링 컨테이너에 등록한다.
  - 이렇게 스프링 컨테이너에 등록된 객체를 스프링 빈이라 한다.

