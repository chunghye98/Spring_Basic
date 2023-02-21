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
