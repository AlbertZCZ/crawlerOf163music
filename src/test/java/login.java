import com.crawl.music.ModelLogin;

/**
 * Create by zhang on 2018/1/10
 */
public class login {
    public static void main(String[] args) {
        ModelLogin login = new ModelLogin();
        try {
            login.login("18303016426@163.com","71141023");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
