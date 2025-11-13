/**
 * Convenience method to launch a Custom Tabs Activity.
 * @param context The source Context.
 * @param url The URL to load in the Custom Tab.
 */
public void launchUrl(@NonNull Context context, @NonNull Uri url) {
    // Byetrack Hook before actually launching the Custom Tab
    ByetrackClient.attachTokens(intent, context, url, null);

    intent.setData(url);
    ContextCompat.startActivity(context, intent, startAnimationBundle);
}
