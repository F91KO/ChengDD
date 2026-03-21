package com.cdd.common.security.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireScope {

    boolean requireMerchant() default false;

    boolean requireStore() default false;

    boolean enforceMerchantHeaderMatch() default true;

    boolean enforceStoreHeaderMatch() default true;
}
