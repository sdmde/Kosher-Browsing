# Release builds

The latest app version is 2.8, version code 15. Package ID, min SDK 26, target SDK 36 and the three Gecko ABIs remain unchanged.

Generate a signed release bundle with your own external upload keystore and the KOSHER_UPLOAD_* environment variables described in README.md. Never commit private keys or passwords. Source distributions and this repository intentionally exclude them. A debug APK cannot update an installation signed with Google Play's app-signing certificate; publish the AAB through the same Play Console app/track for those users.

For updates, use a version code higher than every active version that must upgrade. Upload the newly built bundle into the intended release and wait for processing; reusing the existing bundle without adding/removing one creates an empty release. Play Console account declarations, privacy policy URL, testing and review remain the developer's responsibility.

Current builds request no foreground-service permissions. If an older active Play artifact still requests them, its declaration may remain necessary until that artifact is replaced; local cleanup does not delete artifacts from Play Console.
