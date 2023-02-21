# 스프링 컨테이너와 스프링 빈
## 스프링 컨테이너 생성
### 스프링 컨테이너가 생성되는 과정
```java
ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
```
- ApplicationContext를 스프링 컨테이너라고 한다.
  - 이것은 인터페이스
  - AnnotationConfigApplicationContext 이 구현체 
    - annotation 기반임을 알 수 있다.
- 스프링 컨테이너는 요즘 애노테이션 기반의 자바 설정 클래스로 만든다.

> 컨테이너: 사용하는 객체를 담고 있다. 일반적으로 ApplicationContext를 스프링 컨테이너라 한다. BeanFactory도 있지만 직접 사용하는 경우는 거의 없다.

1. 스프링 컨테이너 생성(AppConfig.class)
2. 스프링 컨테이너가 구성 정보(AppConfig.class)를 활용해서 생성된다.
3. 스프링 컨테이너는 파라미터로 넘어온 설정 클래스 정보를 사용해서 스프링 빈을 등록한다.
   1. key: method name, value: 빈 객체
        - 빈 이름은 항상 다른 이름을 부여해야 한다.
        - 빈 이름을 직접 부여할 수도 있다.
4. 스프링 빈 의존관계 설정 - 준비
   1. 스프링 빈 생성(?)
5. 스프링 빈 의존관계 설정 - 완료
   1. 설정 정보를 참고해서 의존관계를 주입(DI)한다.
   
## 컨테이너에 등록된 모든 빈 조회
test/../core/beanfind/ApplicationContextInfoTest.java
```java
public class ApplicationContextInfoTest {
    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);

    @Test
    @DisplayName("모든 빈 출력하기")
    void findAllBean() {
        String[] beanDefinitionNames = ac.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = ac.getBean(beanDefinitionName);
            System.out.println("name = " + beanDefinitionName + " object = " + bean);
        }
    }

    @Test
    @DisplayName("애플리케이션 빈 출력하기")
    void findApplicationBean() {
        String[] beanDefinitionNames = ac.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = ac.getBeanDefinition(beanDefinitionName);

            if (beanDefinition.getRole() == BeanDefinition.ROLE_APPLICATION) {
                Object bean = ac.getBean(beanDefinitionName);
                System.out.println("name = " + beanDefinitionName + " object = " + bean);
            }
        }
    }
}
```
- 모든 빈 출력하기
  - 스프링에 등록된 모든 빈 정보를 출력
  - ac.getBeanDefinitionNames() : 스프링에 등록된 모든 빈 이름 조회
  - ac.getBean() : 빈 이름으로 빈 객체(인스턴스)조회
- 애플리케이션 빈 출력하기
  - 내가 등록한 빈만 출력
  - 스프링이 내부에서 사용하는 빈은 getRole()로 구분 가능하다
    - ROLE_APPLICATION : 일반적으로 사용자가 정의한 빈
    - ROLE_INFRASTRUCTURE : 스프링이 내부에서 사용하는 빈

## 스프링 빈 조회 - 기본
스프링 컨테이너에서 스프링 빈을 찾는 가장 기본적인 조회 방법
- ac.getBean(빈이름, 타입)
- ac.getBean(타입)    

test/../beanfind/ApplicationContextBasicFindTest.java
```java
public class ApplicationContextBasicFindTest {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);

    @Test
    @DisplayName("빈 이름으로 조회")
    void findBeanByName() {
        MemberService memberService = ac.getBean("memberService", MemberService.class);
        assertThat(memberService).isInstanceOf(MemberServiceImpl.class);
    }

    @Test
    @DisplayName("이름 없이 타입으로만 조회")
    void findBeanByType() {
        MemberService memberService = ac.getBean(MemberService.class);
        assertThat(memberService).isInstanceOf(MemberServiceImpl.class);
    }

    @Test
    @DisplayName("구체 타입으로 조회")
    void findBeanByName2() {
        MemberService memberService = ac.getBean("memberService", MemberServiceImpl.class);
        assertThat(memberService).isInstanceOf(MemberServiceImpl.class);
    }

    @Test
    @DisplayName("빈 이름으로 조회 X")
    void findBeanByNameX() {
//        MemberService xxxx = ac.getBean("xxxx", MemberService.class);
        Assertions.assertThrows(NoSuchBeanDefinitionException.class,
                () -> ac.getBean("xxxx", MemberService.class));
    }
}
```
- 구체타입으로만도 조회할 수 있다.
  - 변경시 유연성이 떨어진다.
- 항상 테스트는 실패 테스트도 만들어야 한다.

## 스프링 빈 조회 - 동일한 타입이 둘 이상
- 같은 타입의 스프링 빈이 둘 이상이면 오류가 발생
  - __빈 이름 지정!__
- ac.getBeansOfType()을 사용하면 해당 타입의 모든 빈 조회 가능    

test/../beanfind/ApplicationContextSameBeanFindTest.java

```java
public class ApplicationContextSameBeanFindTest {
    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(SameBeanConfig.class);

    @Test
    @DisplayName("타입으로 조회 시 같은 타입이 둘 이상 있으면 중복 오류가 발생한다.")
    void findBeanByTypeDuplicate() {
//        MemberRepository bean = ac.getBean(MemberRepository.class);
        assertThrows(NoUniqueBeanDefinitionException.class,
                () -> ac.getBean(MemberRepository.class));
    }

    @Test
    @DisplayName("타입으로 조회시 같은 타입이 둘 이상 있으면, 빈 이름을 지정하면 된다.")
    void findBeanByName() {
        MemberRepository memberRepository = ac.getBean("memberRepository1", MemberRepository.class);
        assertThat(memberRepository).isInstanceOf(MemberRepository.class);
    }

    @Test
    @DisplayName("특정 타입을 모두 조회하기")
    void findAllBeanByType() {
        Map<String, MemberRepository> beansOfType = ac.getBeansOfType(MemberRepository.class);
        for (String s : beansOfType.keySet()) {
            System.out.println("key = " +s+" value = "+beansOfType.get(s));
        }
        System.out.println("beansOfType = "+beansOfType);
        assertThat(beansOfType.size()).isEqualTo(2);
    }

    @Configuration
    static class SameBeanConfig {

        @Bean
        public MemberRepository memberRepository1() {
            return new MemoryMemberRepository();
        }

        @Bean
        public MemberRepository memberRepository2() {
            return new MemoryMemberRepository();
        }
    }
}
```

## 스프링 빈 조회 - 상속 관계 <- 중요!
- 부모 타입으로 조회하면, 자식 타입도 함께 조회한다.
- 모든 자바 객체의 최고 부모인 Object 타입으로 조회하면, 모든 스프링 빈을 조회한다.    

test/../beanfind/ApplicationContextExtendsFindTest.java
```java
public class ApplicationContextExtendsFindTest {
    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(TestConfig.class);

    @Test
    @DisplayName("부모 타입으로 조회시, 자식이 둘 이상 있으면 중복 오류가 발생한다.")
    void findBeanByParentTypeDuplicate() {
        assertThrows(NoUniqueBeanDefinitionException.class,
                () -> ac.getBean(DiscountPolicy.class));
    }

    @Test
    @DisplayName("부모 타입으로 조회시, 자식이 둘 이상 있으면 빈 이름을 지정하면 된다.")
    void findBeanByParentTypeBeanName() {
        DiscountPolicy rateDiscountPolicy = ac.getBean("rateDisCountPolicy", DiscountPolicy.class);
        assertThat(rateDiscountPolicy).isInstanceOf(RateDiscountPolicy.class);
    }

    @Test
    @DisplayName("특정 하위 타입으로 조회")
    void findBeanBySubType() {
        RateDiscountPolicy bean = ac.getBean(RateDiscountPolicy.class);
        assertThat(bean).isInstanceOf(RateDiscountPolicy.class);
    }

    @Test
    @DisplayName("부모 타입으로 모두 조회하기")
    void findAllBeanByParentType() {
        Map<String, DiscountPolicy> beansOfType = ac.getBeansOfType(DiscountPolicy.class);
        assertThat(beansOfType.size()).isEqualTo(2);
        for (String key : beansOfType.keySet()) {
            System.out.println("key = " + key + " value = " + beansOfType.get(key));
        }
    }

    @Test
    @DisplayName("부모 타입으로 모두 조회하기 - Object")
    void findAllBeanByObjectTYpe() {
        Map<String, Object> beansOfType = ac.getBeansOfType(Object.class);
        for (String key : beansOfType.keySet()) {
            System.out.println("key = " + key + " value = " + beansOfType.get(key));
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        public DiscountPolicy rateDisCountPolicy() {
            return new RateDiscountPolicy();
        }

        @Bean
        public DiscountPolicy fixDiscountPolicy() {
            return new FixDiscountPolicy();
        }
    }
}
```
- 실제 개발 환경에서 이렇게 ApplicationContext를 조회하는 경우는 거의 없다.


## BeanFactory와 ApplicationContext
<img width="362" alt="beanfactory" src="https://user-images.githubusercontent.com/57451700/220343200-9e01e05c-31ce-46d2-88b3-178d68a04db6.png">    
이 두 개를 스프링 컨테이너라 한다.

__BeanFactory__
- 스프링 컨테이너의 최상위 인터페이스
- 스프링 빈을 관리하고 조회하는 역할
- getBean() 제공 
- 직접 사용할 일은 없다

__ApplicationContext__
- BeanFactory의 기능을 모두 상속받아서 제공
- 부가기능 제공

### ApplicationContext가 제공하는 부가기능
- 메시지소스를 활용한 국제화 기능
- 환경변수
- 애플리케이션 이벤트
- 편리한 리소스 조회

## 다양한 설정 형식 지원 - 자바 코드, XML
- 스프링 컨테이너는 다양한 형식의 설정 정보를 받아드릴 수 있게 유연하게 설계되어 있다.    

지금까지 했던거랑 비슷해서 정리는 안함, 필요하면 스프링 공식 레퍼런스 문서 살펴보기

## 스프링 빈 설정 메타 정보 - BeanDefinition
- 스프링에서 다양한 설정 형식을 지원할 수 있는 이유
  - __BeanDefinition__ 이라는 추상화를 사용했기 때문
  - __역할과 구현을 개념적으로 나눈 것__
  - 이 설정 BeanDefinition을 빈 설정 메타정보라 한다.
  - 스프링 컨테이너는 이 메타정보를 기반으로 스프링 빈을 생성한다.
    - xml인지 자바 코드인지 몰라도 된다.
    
- AnnotationConfigApplicationContext는 AnnotatedBeanDefinitionReader를 사용해서 AppConfig.class를 읽고 BeanDefinition을 생성한다. 
- 새로운 형식의 설정 정보가 추가되면, XxxBeanDefinitionReader를 만들어서 BeanDefinition을 생성하면 된다.

### BeanDefinition 살펴보기
test/../core/beandefinition/BeanDefinitionTest.java
```java
public class BeanDefinitionTest {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);

    @Test
    @DisplayName("빈 설정 메타정보 확인")
    void findApplicationBean() {
        String[] beanDefinitionNames = ac.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = ac.getBeanDefinition(beanDefinitionName);
            if (beanDefinition.getRole() == BeanDefinition.ROLE_APPLICATION) {
                System.out.println("beanDefinitionName = " + beanDefinitionName + " beanDefinition = " + beanDefinition);
            }
        }
    }
}
```
- BeanDefinition을 직접 생성해서 스프링 컨테이너에 등록할 수 있으나 실무에서 거의 사용할 일 없다.
















