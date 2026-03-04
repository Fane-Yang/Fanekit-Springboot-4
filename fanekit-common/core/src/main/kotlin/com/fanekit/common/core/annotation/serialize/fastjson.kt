package com.fanekit.common.core.annotation.serialize


// fastjson注解使用说明
// 对于返回数据，时间需要序列化给字段指定下面的注解即可
// @JSONField( format = "yyyy-MM-dd hh:mm:ss")
// 指定属性不参与序列化，在序列化时会丢弃指定字段
// @JSONField(serialize=false)

// 指定序列化后的字段名(用于将原本的字段名序列化时修改为xxx)
// @JSONField(name= "XXX")

// @JSONField(ordinal = 1) — 指定序列化顺序

// 1、@JSONField用在字段上影响序列化/反序列化两个过程；
// 2、@JSONField用在setXXX、getXXX方法上，分别影响反序列化、序列化过程；
