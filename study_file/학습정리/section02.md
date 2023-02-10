# 스프링 핵심 원리 이해1 - 예제 만들기
백문이 불여일타..ㅎ
## 프로젝트 생성
스프링 없이 순수 자바 코드로 이루어진 프로젝트

- Java 11
- IntelliJ
- Gradle
- Spring Boot 2.7.8
- Packaging: Jar
- Project Metadata
  - groupId: hello
  - artifactId: core
- Dependencies: x

## 비즈니스 요구사항과 설계
### 회원
- 가입
- 조회
- 등급: 일반, VIP
- 회원 데이터는 자체 DB를 구축할 수 있고, 외부 시스템과 연동할 수 있다.
  - 인터페이스로 역할과 구현 구분
  
### 주문과 할인 정책
- 회원은 상품을 주문할 수 있따.
- 회원 등급에 따라 할인 정책 적용 가능
- 할인 정책은 모든 VIP는 1000원을 할인해주는 고정 금액 할인 적용(추후 변경 가능)
- 할인 정책은 변경 가능성이 높다. 
  - 회사의 기본 할인 정책을 아직 정하지 않았다.
  - 오픈 전까지 고민을 미룰 것이다.
  - 최악의 경우 할인을 적용하지 않을 수도 있다.
  - __인터페이스 만들어서 구현체 갈아끼울 수 있도록 설계__
  
## 회원 도메인 설계
### 회원 도메인 요구사항
- 회원을 가입하고 조회할 수 있다.
- 회원은 일반과 VIP 두 가지 등급이 있다.
- 회원 데이터는 자체 DB를 구축할 수 있고, 외부 시스템과 연동할 수 있다.

<img width="354" alt="g" src="https://user-images.githubusercontent.com/57451700/218091472-1e74572e-4687-47c0-a18d-73db53f33cc5.png">

## 회원 도메인 개발
hello/core/member/Grade.java
```java
public enum Grade {
    BASIC,
    VIP
}
```
hello/core/member/Member.java
```java
public class Member {
    private Long id;
    private String name;
    private Grade grade;

    public Member(Long id, String name, Grade grade) {
        this.id = id;
        this.name = name;
        this.grade = grade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }
}
```
hello/core/member/MemberRepository.java
```java
public interface MemberRepository {
    void save(Member member);

    Member findById(Long memberId);
}
```
hello/core/member/MemoryMemberRepository.java
```java
public class MemoryMemberRepository implements MemberRepository{

    private static Map<Long, Member> store = new HashMap<>();
    
    @Override
    public void save(Member member) {
        store.put(member.getId(), member);
    }

    @Override
    public Member findById(Long memberId) {
        return store.get(memberId);
    }
}
```
- 실무에서는 동시성 이슈가 있어서 ConcurrentHashMap을 써야 한다.
- 여기서는 그냥 HashMap을 사용한다.

> ConcurrentHashMap:    
> - 내부적 동기화 때문에 Thread Safe하다.    
> - 추가 및 삭제와 같은 수정 작업만 동기화 된다.    
> - null 키와 null 값을 허용하지 않는다.    

hello/core/member/MemberService.java
```java
public interface MemberService {
    void join(Member member);

    Member findMember(Long MemberId);
}
```

hello/core/member/MemberServiceImpl.java
```java
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository = new MemoryMemberRepository();
    
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
- 관례상 구현체가 하나 존재하는 경우는 interface 명 뒤에 impl을 붙인다.

## 회원 도메인 실행과 테스트
hello/core/MemberApp.java
```java
public class MemberApp {
    public static void main(String[] args) {
        MemberService memberService = new MemberServiceImpl();
        Member member = new Member(1L, "memberA", Grade.VIP);
        memberService.join(member);

        Member findMember = memberService.findMember(1L);
        System.out.println("new member = " + member.getName());
        System.out.println("findMember = " + findMember.getName());
    }
}
```
- 순수한 자바코드로 테스트 한 코드
- main 메서드 안에서 테스트 하는 것은 한계가 있다.
- Junit이라는 테스트 프레임워크 사용!

<br/>

test/java/hello/core/member/MemberServiceTest.java
```java
class MemberServiceTest {

    MemberService memberService = new MemberServiceImpl();

    @Test
    void join() {
        //given
        Member member = new Member(1L, "memberA", Grade.VIP);

        //when
        memberService.join(member);
        Member findMember = memberService.findMember(1L);

        //then
        assertThat(member).isEqualTo(findMember);
    }

    @Test
    void findMember() {
    }
}
```
- 빌드할 때 테스트코드는 빼고 main 안에 있는 코드만 운영 환경에 배포된다.

### 회원 도메인 설계의 문제점
- 다른 저장소로 변경할 때 OCP 원칙을 잘 준수하는가?
- DIP를 잘 지키고 있는가?
- __MemberService가 interface 뿐만 아니라 구현체까지 의존하고 있는 문제가 있음__
  - 추상화와 구체화에 모두 의존
  - DIP 위반
  - 변경할 때 문제 생김

## 주문과 할인 도메인 설계
### 주문과 할인 정책
- 회원은 상품을 주문할 수 있다.
- 회원 등급에 따라 할인 정책을 적용할 수 있다.
- 할인 정책은 모든 VIP는 1000원을 할인해주는 고정 금액 할인을 적용한다.
  - 나중에 변경 가능
- 할인 정책은 변경 가능성이 높다. 회사의 기본 할인 정책을 아직 정하지 못했다.
  - 최악의 경우 할인 적용하지 않을 수도 있다.
  - 인터페이스 만들기

### 주문 Flow
<img width="355" alt="gg" src="https://user-images.githubusercontent.com/57451700/218102768-ce74b851-92d3-4e84-bd66-a776d17649ab.png">

1. __주문 생성__ : 클라이언트는 주문 서비스에 주문 생성을 요청한다.
2. __회원 조회__ : 할인을 위해서는 회원 등급이 필요하다. 그래서 주문 서비스는 회원 저장소에서 회원을 조회한다.
3. __할인 적용__ : 주문 서비스는 회원 등급에 따른 할인 여부를 정책에 위임한다.
4. __주문 결과 반환__ : 주문 서비스는 할인 결과를 포함한 주문 결과를 반환한다.     
   - 지금은 실제 DB에 적용하는 것은 복잡하니까 생략!

## 주문과 할인 도메인 개발
hello/core/discount/DiscountPolicy.java
```java
public interface DiscountPolicy {

    /**
     * @return 할인 대상 금액
     */
    int discount(Member member, int price);
}
```

hello/core/discount/FixDiscountPolicy.java
```java
public class FixDiscountPolicy implements DiscountPolicy{
    private int discountFixAmount = 1000; // 1000원 할인

    @Override
    public int discount(Member member, int price) {
        if (member.getGrade() == Grade.VIP) {
            return discountFixAmount;
        } else {
            return 0;
        }
    }
}
```

hello/core/order/Order.java
```java
public class Order {

    private Long memberId;
    private String itemName;
    private int itemPrice;
    private int discountPrice;

    public Order(Long memberId, String itemName, int itemPrice, int discountPrice) {
        this.memberId = memberId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.discountPrice = discountPrice;
    }

    public int calculatePrice() {
        return itemPrice - discountPrice;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(int itemPrice) {
        this.itemPrice = itemPrice;
    }

    public int getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(int discountPrice) {
        this.discountPrice = discountPrice;
    }

    @Override
    public String toString() {
        return "Order{" +
                "memberId=" + memberId +
                ", itemName='" + itemName + '\'' +
                ", itemPrice=" + itemPrice +
                ", discountPrice=" + discountPrice +
                '}';
    }
}
```

order/OrderService.java
```java
public interface OrderService {
    Order createOrder(Long memberId, String itemName, int itemPrice);
}
```

order/OrderServiceImpl.java
```java
public class OrderServiceImpl implements OrderService {
    private final MemberRepository memberRepository = new MemoryMemberRepository();
    private final DiscountPolicy discountPolicy = new FixDiscountPolicy();
    
    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int discountPrice = discountPolicy.discount(member, itemPrice);
        
        return new Order(memberId, itemName, itemPrice, discountPrice);
    }
}
```

## 주문과 할인 도메인 실행과 테스트
- main 메서드로 동작 확인
core/OrderApp.java
```java
public class OrderApp {
    public static void main(String[] args) {
        MemberService memberService = new MemberServiceImpl();
        OrderService orderService = new OrderServiceImpl();

        Long memberId = 1L;
        Member member = new Member(memberId, "memberA", Grade.VIP);
        memberService.join(member);

        Order order = orderService.createOrder(memberId, "itemA", 10000);

        System.out.println("order = " + order);
    }
}
```

test/java/hello/core/order/OrderServiceTest.java
```java
class OrderServiceTest {
    MemberService memberService = new MemberServiceImpl();
    OrderService orderService = new OrderServiceImpl();

    @Test
    void createOrder() {
        Long memberId = 1L;
        Member member = new Member(memberId, "memberA", Grade.VIP);
        memberService.join(member);

        Order order = orderService.createOrder(memberId, "itemA", 10000);
        Assertions.assertThat(order.getDiscountPrice()).isEqualTo(1000);
    }
}
```



