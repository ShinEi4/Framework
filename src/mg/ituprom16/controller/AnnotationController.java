package mg.ituprom16.controller;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface  AnnotationController{
    String nom() default "";
}
