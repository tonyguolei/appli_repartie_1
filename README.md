appli_repartie ENSIMAG
==============
Configuration des serveurs
Serveur :
Modifier le fichier de configuration dans Servers/target/ConfigServer.properties
Client :
Modifier le fichier de configuration dans Users/target/ConfigServer.properties

Initialisation des serveurs
1. Accéder au répertoire Servers/target
2. java -jar servers-1.0-SNAPSHOT.jar (4 fois pour les 4 serveurs au début)
3.
-pour S1: taper “1” pour signaler un nouveau serveur,
taper “1” pour le numéro du serveur
-pour S2: taper “1” pour signaler un nouveau serveur
taper “2” pour le numéro du serveur
-pour S3: taper “1” pour signaler un nouveau serveur
 taper “3” pour le numéro du serveur
-pour S4: taper “1” pour signaler un nouveau serveur
 taper “4” pour le numéro du serveur
(Le serveur master a le mot clé « Master » en début de phrase »)

Extinction d’un serveur
Taper “kill” dans la console du serveur pour éteindre le serveur

Redémarrage d’un serveur
1. Accéder au répertoire Users/target
2. java -jar client-1.0-SNAPSHOT.jar
3. taper “2” pour signaler la reconnexion d’un ancien serveur
4. taper le numéro de ce serveur (par exemple, si le serveur ressuscité est S1, tapez “1”)

Récapitulatif de la gestion d’un serveur
Taper “help” dans la console du serveur pour afficher les commandes suivantes:
-kill              	 		Arrêter le serveur
-master             		Afficher le serveur master
-neighborBehind     		Afficher le serveur derrière ce serveur
-neighborFront      		Afficher le serveur devant ce serveur
-usersConnected     		Afficher les utilisateurs connectés
-usersDisconnected  	Afficher les utilisateurs déconnectés
-usersPlaying       		Afficher les utilisateurs qui sont en train de jouer
-userWaiting       		Afficher l'utilisateur en attente
-game               		Afficher les parties en cours
-unfinishedGame	Afficher les parties non terminées (avec déconnexion / panne joueur)

Démarrage d’un client
1. accéder au répertoire Users/target
2. java -jar client-1.0-SNAPSHOT.jar
3. Cliquer le bouton “Se connecter” du client pour connecter au serveur
Attention !
Chaque client doit avoir un pseudo unique
Les serveurs doivent tous être connectés. (Affichage des messages « Demande acceptée » et « Initialisation des serveurs OK » sur le serveur master.

Utilisation de l’interface
“Se déconnecter” : quitter le jeu
“X” : simuler une panne (en fermant la fenêtre)
“Jouer” : demander le lancement d’une nouvelle partie
Cliquer sur la réponse de votre choix pour répondre à la question posée

