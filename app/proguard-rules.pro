-dontwarn org.commonmark.ext.gfm.strikethrough.Strikethrough

# Ephyra: strip verbose and debug logging from release builds; info, warn and error stay for log reports
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}
-assumenosideeffects class timber.log.Timber$Forest {
    public void v(...);
    public void d(...);
}
