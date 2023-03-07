# 의존관계 자동 주입
## 다양한 의존관계 주입 방법
__의존관계 주입 방법__
- 생성자 주입
- 수정자 주입(setter 주입)
- 필드 주입
- 일반 메서드 주입

### 생성자 주입
- 생성자를 통해 의존관계를 주입
- 특징
  - 생성자 호출 시점에 딱 1번만 호출되는 것이 보장
  - __불변, 필수__ 의존 관계에 사용
    - final 키워드가 있는데 생성자가 없으면 컴파일 오류 생김
  - 생성자가 1개만 있으면 @Autowired를 생략해도 자동 주입된다.

### 수정자 주입(setter 주입)
- setter라 불리는 필드의 값을 변경하는 수정자 메서드를 통해서 의존관계를 주입
- 특징
  - __선택, 변경__ 가능성이 있는 의존관계에 사용
  - @Autowired(required = false) : 주입할 대상이 없으면 무시하고 동작하도록 만듦

### 필드 주입
- 필드에 @Autowired 붙임
- 특징
  - 코드가 간결하지만 외부에서 변경이 불가능해서 테스트하기 힘들다
  - DI 프레임워크가 없으면 아무것도 할 수 없다
  - 사용하지 말자!
    - 테스트코드나 스프링 설정을 위한 @Configuration 같은 곳에서만 특별한 용도로 사용
- 필드 주입을 하면 setter를 만들 수밖에 없다.

### 일반 메서드 주입
- 일반 메서드에서 주입
- 특징
  - 한번에 여러 필드를 주입받을 수 있다.
  - 일반적으로 잘 사용하지 않는다.

## 옵션 처리
- 주입할 스프링 빈이 없어도 동작해야 할 때가 있다.
- @Autowired의 required 기본 옵션값이 true로 되어 있어서 자동 주입 대상이 없으면 오류가 발생한다.

### 처리방법
- @Autowired(required = false) : 자동 주입할 대상이 없으면 수정자 메서드 자체가 호출 안됨
- @Nullable : 자동 주입할 대상이 없으면 null 입력
- Optional<> : 자동 주입할 대상이 없으면 Optional.empty 가 입력된다.

## 생성자 주입을 선택해라!
최근에는 스프링을 포함한 DI 프레임워크 대부분이 생성자 주입을 권장한다.    
__불변__
- 대부분의 의존 관계 주입은 한 번 일어나면 애플리케이션 종료 시점까지 의존 관계를 변경할 일이 없다.
- 수정자 주입을 사용하면, setXXX 메서드를 public 으로 열어두어야 한다.
- 생성자 주입은 객체를 생성할 때 딱 1번만 호출되므로 이후에 호출되는 일이 없다. 따라서 불변하게 설계할 수 있다.

__누락__    
- 수정자에 의존관계 주입을 누락하면 NPE가 발생한다.
- 생성자 주입을 사용하면 주입 데이터를 누락하면 컴파일 오류가 발생한다.
- 테스트를 실행해보지 않아도 IDE에서 어떤 값을 필수로 주입해야 하는지 알 수 있다.

### final 키워드
- final 키워드를 사용하면 생성자 주입을 하지 않았을 때 컴파일 단계에서 오류가 난다.
- 수정자 주입을 포함한 나머지 주입 방식은 모두 생성자 이후에 호출되므로, 필드에 final 키워드를 사용할 수 없다.

## 롬복과 최신 트렌드
- 생성자가 1개만 있으면 @Autowired를 생략할 수 있다.
__롬복 설정__
```
//lombok 설정 추가 시작
configurations {
 compileOnly {
 extendsFrom annotationProcessor
 }
}
//lombok 설정 추가 끝

dependencies {
 //lombok 라이브러리 추가 시작
 compileOnly 'org.projectlombok:lombok'
 annotationProcessor 'org.projectlombok:lombok'
 testCompileOnly 'org.projectlombok:lombok'
 testAnnotationProcessor 'org.projectlombok:lombok'
 //lombok 라이브러리 추가 끝
}
```
1. 설정 -> plugin -> lombok 검색 후 설치
2. 설정 -> Annotation Processors -> enable annotation processing 체크

```java
@Getter @Setter // 추가
public class Member {
    private Long id;
    private String name;
    private Grade grade;

    public Member(Long id, String name, Grade grade) {
        this.id = id;
        this.name = name;
        this.grade = grade;
    }
}
```
- 어노테이션으로 getter/setter를 사용할 수 있다.

```java
@Component
@RequiredArgsConstructor // 추가
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository;
    private final DiscountPolicy discountPolicy;

    // 생성자 삭제
    
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
- @RequiredArgsConstructor 으로 final 키워드가 붙은 것들을 자동으로 생성자 주입이 되게 한다.
- 의존관계 추가할 때 아주 좋다.

## 조회 빈이 2개 이상 - 문제
@Autowired는 type으로 조회한다.
- 선택된 빈이 2개 이상일 때 문제가 발생한다.
- 하위타입으로 지정하는 것은 DIP를 위배하고 유연성이 떨어진다.
- 의존관계 자동 주입으로 해결할 수 있는 방법이 있다.

## @Autowired 필드명, @Qualifier, @Primary
조회 대상 빈이 2개 이상일 때 해결방법
- @Autowired 필드명 매칭
- @Qualifier -> @Qualifier끼리 매칭 -> 빈 이름 매칭
- @Primary 사용

### @Autowired 필드명 매칭
타입 매칭을 시도하고, 여러 빈이 있으면 필드 이름, 파라미터 이름으로 빈 이름을 추가 매칭한다.


### @Quilifier 사용
추가 구분자를 붙여주는 방법, 주입 시 추가적인 방법을 제공하는 것이지 빈 이름을 변경하는 것은 아니다.
- @Qualifier는 @Qualifier를 찾는 용도로만 사용하는게 명확하고 좋다.

### @Primary 사용
같은 이름의 빈이 있을 때 우선순위를 최상으로 주입할 수 있게 만든다.
- 메인 데이터베이스를 가져오는 connection을 할 때 @Primary 걸어주는 방식으로 사용 가능하다.

> 우선순위: 항상 자세한 것이 우선권을 가진다. 따라서 @Primary보다 @Qualifier가 우선순위가 더 높다.

## 애노테이션 직접 만들기
@Qualifier("mainDiscountPolicy") 이렇게 문자를 적으면 컴파일 시 타입 체크가 안된다. 
- 애노테이션을 만들어서 문제 해결 가능

hello/core/annotation/MainDiscountPolicy.java
```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier("mainDiscountPolicy")
public @interface MainDiscountPolicy {
}
```
RateDiscountPolicy
```java
@Component
@MainDiscountPolicy // 편하게 쓰기 가능
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
- 애노테이션은 상속이라는 개념이 없다.
- 애노테이션을 모아서 사용하는 기능은 스프링이 지원하는 기능

## 조회한 빈이 모두 필요할 때 List, Map
의도적으로 해당 타입의 스프링 빈이 모두 필요할 때가 있다.
- 전략 패턴 사용!

## 자동, 수동의 올바른 실무 운영 기준
- 편리한 자동 기능을 기본적으로 사용하자
  - 스프링부트는 컴포넌트 스캔을 기본으로 사용한다.
  - 관리할 빈이 많아서 설정 정보가 커지면 설정 정보를 관리하는 것 자체가 부담이 된다.
  - 자동 빈 등록을 해도 OCP, DIP를 지킬 수 있다.
- 수동 빈 등록을 사용하는 경우
  - 업무 로직을 지원하기 위한 하부 기술이나 공통 기술지원 로직에 사용하면 좋다.
  - 업무 로직은 유사한 패턴이 있기 때문에 자동 기능을 적극 사용하는 것이 좋다.
  - 기술 지원 로직은 그 수가 매우 적어서 적용이 잘 되고 있는지 파악하기 어려운 경우가 많기 때문에 수동 빈 등록을 사용하는 것이 좋다.
- 비즈니스 로직 중에서 다형성을 적극 활용할 때 수동 빈 등록을 사용하면 좋다.
  - 설정 정보만 봐도 한 눈에 빈의 이름과 어떤 빈들이 주입될지 파악할 수 있기 때문이다.
  - 자동으로 하면 특정 패키지에 같이 묶어둬야 한 눈에 이해할 수 있다.
- 스프링과 스프링부트가 자동으로 등록하는 빈들은 예외다.
  - ex. DataSource(데이터베이스 연결에 사용하는 기술 지원 로직)
- 