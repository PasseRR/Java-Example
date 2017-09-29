package sun.misc;

/**
 * @author xiehai1
 * @date 2017/09/29 11:41
 * @Copyright(c) gome inc Gome Co.,LTD
 */
public class AnonymousClass {
    private Long id;
    private static final String NAME = "AnonymousClass";
    public AnonymousClass(){
        this.id = 1000L;
    }

    public static String getName(){
        return NAME;
    }

    public Long getId(){
        return this.id;
    }
}