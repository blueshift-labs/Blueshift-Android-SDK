# Address Gson errors with TypeToken
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Address Gson errors with abstract classes
-keep public class * implements java.io.Serializable

# Do not shrink the members of the following classes
-keepclassmembernames class com.blueshift.batch.BulkEvent {*;}
-keepclassmembernames class com.blueshift.model.Subscription {*;}
-keepclassmembernames class com.blueshift.model.UserInfo {*;}
-keepclassmembernames class com.blueshift.rich_push.Action {*;}
-keepclassmembernames class com.blueshift.rich_push.Message {*;}
-keepclassmembernames class com.blueshift.rich_push.CarouselElement {*;}
-keepclassmembernames class com.blueshift.rich_push.CarouselElementText {*;}
