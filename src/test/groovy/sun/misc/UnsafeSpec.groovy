package sun.misc

import spock.lang.Shared
import spock.lang.Specification

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field
/**
 * @author xiehai1
 * @date 2017/09/28 17:44
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */
class UnsafeSpec extends Specification {
    @Shared
    Unsafe unsafe

    def setupSpec() {
        // 通过反射获得Unsafe实例
        Field field = Unsafe.class.getDeclaredField("theUnsafe")
        field.setAccessible(true)
        unsafe = (Unsafe) field.get(null)
    }

    def cleanupSpec() {
        unsafe = null
    }

    def unsafe() {
        expect:
        unsafe != null
    }

    def "addressSize"() {
        given:
        def size = unsafe.addressSize()
        expect:
        size == 4 || size == 8
    }

    def "allocateInstance"() {
        given:
        def student = (Student) unsafe.allocateInstance(Student.class)

        expect:
        student != null
        student.name.is(null)
        student.age == 0
    }

    def "allocateMemory"() {
        given:
        def address = unsafe.allocateMemory(10)
        def binary = Long.toBinaryString(address)
        def bytes = int.((binary.length() + 1) / 8)

        expect:
        address != 0
        // bytes of address
        bytes == 4 || bytes == 8
    }

    def "arrayBaseOffset"() {
        given:
        def offset = unsafe.arrayBaseOffset(String[].class)
        expect:
        offset == 16
    }

    def "arrayIndexScale"() {
        given:
        def offset = unsafe.arrayIndexScale(byte[].class)
        expect:
        offset == 1
    }

    def "compareAndSwapInt"() {
        given:
        def student = new Student()
        expect:
        student.age == 1

        when:
        def offset = unsafe.objectFieldOffset(Student.class.getDeclaredField("age"))
        // cas修改age为10
        unsafe.compareAndSwapInt(student, offset, 1, 10)
        then:
        student.age == 10
    }

    def "compareAndSwapLong"() {
        given:
        def student = new Student()
        expect:
        student.userId == 21341314

        when:
        def offset = unsafe.objectFieldOffset(Student.class.getDeclaredField("userId"))
        // cas修改userId为10068
        unsafe.compareAndSwapLong(student, offset, student.userId, 10068)
        then:
        student.userId == 10068
    }

    def "compareAndSwapObject"() {
        given:
        def student = new Student()
        expect:
        student.name == "Chen"

        when:
        def offset = unsafe.objectFieldOffset(Student.class.getDeclaredField("name"))
        // cas修改name为Jack
        unsafe.compareAndSwapObject(student, offset, student.name, "Jack")
        then:
        student.name == "Jack"
    }

    def "copyMemory(long srcAddress, long destAddress, long bytes)"() {
        // TODO finish me
        expect:
        true
    }

    def "copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes)"() {
        // TODO finish me
        expect:
        true
    }

    static getClassBytes(){
        def base = UnsafeSpec.class.getResource("").getPath()
        def file = new File(base + "/AnonymousClass.class")
        int length = (int) file.length()
        byte[] buffer = new byte[length]
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length)
        BufferedInputStream inputStream = null
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file))
            int len = 0;
            while (-1 != (len = inputStream.read(buffer, 0, length))) {
                bos.write(buffer, 0, len)
            }

            return buffer
        } catch (IOException e) {
            throw e
        } finally {
            try {
                inputStream.close()
            } catch (IOException e) {
                throw e
            }
            bos.close()
        }
    }

    def "defineAnonymousClass"() {
        given:
        def buffer = getClassBytes()
        // define anonymous class
        Class<?> proxy = unsafe.defineAnonymousClass(UnsafeSpec.class, buffer, null)
        expect:
        proxy != null

        when:
        MethodHandle methodHandle = MethodHandles.lookup()
            .findStatic(proxy, "getName", MethodType.methodType(String.class))
        then:
        methodHandle != null

        when:
        String name = (String) methodHandle.invokeWithArguments()
        then:
        name == "AnonymousClass"

        when:
        def instance = proxy.newInstance()
        def method = proxy.getMethod("getId")
        def id = method.invoke(instance)
        then:
        id == 1000L
    }

    def "defineClass(String name, byte[] b, int off, int len)"(){
        given:
        def bytes = getClassBytes()

        when:
        def clazz = unsafe.defineClass(null, bytes, 0, bytes.length, null, null)
        then:
        clazz != null
        clazz.is(AnonymousClass.class)
    }

    def "staticFieldBase"(){
        given:
        def address = unsafe.staticFieldBase(AnonymousClass.class.getDeclaredField("NAME"))
        expect:
        address != null
    }

    def "throwException"(){
        given:
        unsafe.throwException(new NullPointerException())

        expect:
        true
    }
}

class Student {
    String name = "Jack"
    String nickName
    int age
    long userId

    Student() {
        this.name = "Chen"
        this.age = 1
        this.userId = 21341314L
    }

    def getName() {
        this.name
    }
}