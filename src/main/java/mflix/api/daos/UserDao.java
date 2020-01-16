package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static com.mongodb.client.model.Aggregates.addFields;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private static String USERS_COLLECTION = "users";

    private static String SESSIONS_COLLECTION = "sessions";

    // public MongoCollection<User> usersCollection;

    public MongoCollection<Document> usersCollection;

    public MongoCollection<Session> sessionsCollection;

    private final UpdateOptions options = new UpdateOptions();

    private CodecRegistry pojoCodecRegistry =
            fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    @Autowired
    public UserDao(MongoClient mongoClient, @Value("sample_mflix") String databaseName) {
        super(mongoClient, databaseName);
        options.upsert(true);
        usersCollection = db.getCollection(USERS_COLLECTION);
        sessionsCollection = db.getCollection(SESSIONS_COLLECTION, Session.class).withCodecRegistry(pojoCodecRegistry);
        Logger log = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        Document thisUserDocument = new Document();
        thisUserDocument.put("name", user.getName());
        thisUserDocument.put("email", user.getEmail());
        thisUserDocument.put("password", user.getHashedpw());
        Document userAlreadyExistsQuery = new Document("email", user.getEmail());
        if (usersCollection.find(userAlreadyExistsQuery).first() == null) {
            usersCollection.insertOne(thisUserDocument);
            return true;
        } else throw new IncorrectDaoOperation("That user already exists");
    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        Session newSession = new Session();
        newSession.setUserId(userId);
        newSession.setJwt(jwt);
        Bson query = new Document("email", userId);
        UpdateResult resultWithUpsert =
                sessionsCollection.updateOne(query, new Document("$set", newSession), options);
        if (resultWithUpsert.getModifiedCount() == 0) {
            return true;
        }
        return false;
    }


    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or n
     */
    public User getUser(String email) {
        List<Bson> userIdFilter = Arrays.asList(match(eq("email", email)));
        Document userDocument = usersCollection.aggregate(userIdFilter).first();
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        if (!(userDocument == null)) {
            User user = new User();
            user.setName(userDocument.get("name").toString());
            user.setEmail(userDocument.get("email").toString());
            user.setHashedpw(userDocument.get("password").toString());
            Map<String, String> preferences = user.getPreferences();
            user.setPreferences(preferences);
            return user;
        } else return null;
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        Session session = null;
        List<Bson> pipeline = Arrays.asList(match(eq("email", userId)));
        if (!(sessionsCollection.aggregate(pipeline).first() == null)) {
            return sessionsCollection.aggregate(pipeline).first();
        }
        return null;
    }

    public boolean deleteUserSessions(String userId) {
        List<Bson> pipeline = Arrays.asList(match(eq("email", userId)));
        if (!(sessionsCollection.aggregate(pipeline).first() == null)) {
            Session sessionToDelete = sessionsCollection.aggregate(pipeline).first();
            sessionsCollection.deleteOne(new Document("jwt", sessionToDelete.getJwt()));
            return true;
        }
        return false;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        List<Bson> pipeline = Arrays.asList(match(eq("email", email)));
        if (!(usersCollection.aggregate(pipeline).first() == null)) {
            this.deleteUserSessions(email);
            usersCollection.deleteOne(new Document("email", email));
            return true;
        }
        return false;
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating
        List<Document> userPreferenceDetails = new ArrayList<>();
        for(Map.Entry<String, ?> eachUserPreference: userPreferences.entrySet()) {
            Document singleUserPreference = new Document();
            singleUserPreference.put(eachUserPreference.getKey(), eachUserPreference.getValue());
            userPreferenceDetails.add(singleUserPreference);
        }
        Bson queryFilter = new Document("email", email);
        if (!(usersCollection.find(queryFilter) == null)) {
            usersCollection.updateOne(queryFilter, set("preferences", userPreferenceDetails), options);
            return true;
        }
        throw new IncorrectDaoOperation("No user");
    }
}