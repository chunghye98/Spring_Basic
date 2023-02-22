# 컴포넌트 스캔
## 컴포넌트 스캔과 의존관계 자동 주입 시작하기
- 등록해야 할 스프링 빈이 많아지면 번거로워지고, 누락될 수 있다.
- 스프링은 설정 정보가 없어도 자동으로 스프링빈을 등록하는 __컴포넌트 스캔__ 이라는 기능을 제공한다.
- 의존관계도 자동으로 주입하는 @Autowired도 제공한다.

main/../core/AutoAppConfig.java
```java
@Configuration
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
) // 기존 설정 정보와 충돌이 날 수 있기 때문에 필터링해준다.
public class AutoAppConfig {
}
```
- 컴포넌스 스캔을 사용하면 @ComponentScan을 설정 정보에 붙여주면 된다.
- @Configuration도 @Component 가 붙어있어서 관리 대상이 되기 때문에 필터링 해준다.
- 컴포넌트 스캔 : @Component 애노테이션이 붙은 클래스를 스캔해서 스프링 빈으로 등록한다.

### @Component 추가
- MemoryMemberRepository
- RateDiscountPolicy
- MemberServiceImpl
  - @Autowired 추가
- OrderServiceImpl
  - @Autowired 추가

```java
@Component
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;

    @Autowired
    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```
- 설정 정보가 없기 때문에 의존관계 주입도 이 클래스 안에서 해결해야 한다.
- @Autowired는 의존관계를 자동으로 주입해준다.

```java
@Component
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository;
    private final DiscountPolicy discountPolicy;

    @Autowired
    public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
        this.memberRepository = memberRepository;
        this.discountPolicy = discountPolicy;
    }
}
```
- @Autowired를 사용하면 생성자에서 여러 의존관계도 한번에 주입받을 수 있다.

test/../core/scan/AutoAppConfig.java
```java
public class AutoAppConfigTest {

    @Test
    void basicScan() {
        ApplicationContext ac = new AnnotationConfigApplicationContext(AutoAppConfig.class);
        MemberService memberService = ac.getBean(MemberService.class);
        Assertions.assertThat(memberService).isInstanceOf(MemberService.class);
    }
}
```
- 잘 동작한다.

### 동작
1. @ComponentScan은 @Component가 붙은 모든 클래스를 스프링 빈으로 등록한다.
   - 스프링 빈의 기본 이름은 클래스명을 사용하되, 맨 앞글자만 소문자를 사용한다.
   - 빈 이름을 직접 지정하고 싶으면 @Component("이름") 이런 식으로 하면 된다.
2. @Autowired 의존관계 자동 주입
   - 스프링 컨테이너가 자동으로 해당 스프링 빈을 찾아서 주입한다.
   - 타입으로 빈을 찾는다.

## 탐색 위치와 기본 스캔 대상
### 탐색할 패키지의 기본 위치 지정
- @ComponentScan(basePackages = "") 로 지정 가능
- basePackageClasses : 지정한 클래스의 패키지를 탐색 시작 위로 지정
- 기본 : @ComponentScan이 붙은 설정 정보 클래스의 패키지가 시작 위치
- __권장 : 설정 정보 클래스의 위치를 프로젝트 최상단에 두는 것__

> @SprintBootApplication : 이 설정 안에 @ComponentScan이 들어있다.

### 컴포넌트 스캔 기본 대상
- @Component
- @Controller : 스프링 MVC 컨트롤러로 인식
- @Service
- @Repository : 스프링 데이터 접근 계층으로 인식, 데이터 계층의 예외를 스프링 예외로 변환해준다.
- @Configuration : 스프링 설정 정보로 인식, 스프링 빈이 싱글톤을 유지하도록 추가 처리

## 필터
- includeFilters : 컴포넌트 스캔 대상을 추가로 지정
- excludeFilters : 컴포넌트 스캔에서 제외할 대상 지정

main/../scan/filter
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyIncludeComponent {
}
```
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyExcludeComponent {
}
```
```java
@MyIncludeComponent
public class BeanA {
}
```
```java
@MyExcludeComponent
public class BeanB {
}
```
```java
public class ComponentFilterAppConfigTest {

    @Test
    void filterScan() {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(ComponentFilterAppConfig.class);
        BeanA beanA = ac.getBean("beanA", BeanA.class);
        assertThat(beanA).isNotNull();

        assertThrows(
                NoSuchBeanDefinitionException.class,
                () -> ac.getBean("beanB", BeanB.class)
        );
    }

    @Configuration
    @ComponentScan(
            includeFilters = @Filter(type = FilterType.ANNOTATION, classes = MyIncludeComponent.class),
            excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = MyExcludeComponent.class)
    )
    static class ComponentFilterAppConfig {

    }
}
```
- Filter type 옵션
  - ANNOTATION : 기본값, 애노테이션을 인식해서 동작한다.
  - ASSIGNABLE_TYPE : 지정한 타입과 자식 타입을 인식해서 동작한다.
  - ASPECTJ : AspectJ 패턴 사용
  - REGEX : 정규 표현식
  - CUSTOM : TypeFilter 라는 인터페이스 구현해서 처리
- 스프링 부트는 기본적으로 컴포넌트 스캔을 제공하므로, 이 기본 설정에 맞춰서 사용하는 것을 권장한다.

## 중복 등록과 충돌
컴포넌트 스캔에서 같은 빈 이름을 등록하면?
- 자동 빈 등록 vs 자동 빈 등록
- 수동 빈 등록 vs 자동 빈 등록

### 자동 빈 등록 vs 자동 빈 등록
- 스프링이 오류를 발생신다.
- ConflictingBeanDefinitionException 예외 발생

### 수동 빈 등록 vs 자동 빈 등록
```java
@Configuration
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
) // 기존 설정 정보와 충돌이 날 수 있기 때문에 필터링해준다.
public class AutoAppConfig {

    @Bean(name = "memoryMemberRepository")
    MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }
}
```
- 수동 빈 등록이 우선권을 가진다(수동 빈이 자동 빈을 오버라이딩 해버린다)
- 이 경우는 보통 여러 설정이 꼬여서 만들어지는 경우가 대부분이다.
- 잡기 어려운 버그가 만들어진다.
- 최근 스프링부트에서는 수동 빈 등록과 자동 빈 등록이 충돌나면 오류가 발생하도록 기본값을 바꿨다.

