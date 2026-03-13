# auth.py - REMOVED (Firebase Auth handles authentication directly)
#
# Mobile apps use Firebase Auth SDK:
#   iOS:  Auth.auth().signIn(withEmail:password:)
#   Android: Firebase.auth.signInWithEmailAndPassword(email, password)
#
# User registration:
#   iOS:  Auth.auth().createUser(withEmail:password:)
#   Android: Firebase.auth.createUserWithEmailAndPassword(email, password)
#
# Token refresh: Handled automatically by Firebase SDK
# Logout: Auth.auth().signOut() / Firebase.auth.signOut()
