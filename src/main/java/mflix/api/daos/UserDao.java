package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.UpdateRequest;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

  private final MongoCollection<User> usersCollection;

  private final MongoCollection<Session> sessionsCollection;

  private final Logger log;

  private final UpdateOptions options = new UpdateOptions();


  @Autowired
  public UserDao(
      MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
    super(mongoClient, databaseName);
    options.upsert(true);
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
    log = LoggerFactory.getLogger(this.getClass());
    sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
  }

  /**
   * Inserts the `user` object in the `users` collection.
   *
   * @param user - User object to be added
   * @return True if successful, throw IncorrectDaoOperation otherwise
   */
  public boolean addUser(User user) throws IncorrectDaoOperation {
    Document userAlreadyExistsQuery = new Document("name", user.getName());
    if (usersCollection.find(userAlreadyExistsQuery).first() == null) {
      usersCollection.insertOne(user);
      return true;
    } else {
      throw new IncorrectDaoOperation("That user already exists");
    }
  }

  /**
   * Creates session using userId and jwt token.
   *
   * @param userId - user string identifier
   * @param jwt - jwt string token
   * @return true if successful
   */
  public boolean createUserSession(String userId, String jwt) {
    Session newSession = new Session();
    newSession.setUserId(userId);
    newSession.setJwt(jwt);
    Bson query = new Document("userId", userId);
    UpdateResult resultWithUpsert =
            sessionsCollection.updateOne(query, new Document("$set", newSession), options);
    if(resultWithUpsert.getModifiedCount() == 0) {
      return true;
    }
    return false;
  }


  /**
   * Returns the User object matching the an email string value.
   *
   * @param email - email string to be matched.
   * @return User object or null.
   */
  public User getUser(String email) {
    User user = null;
    List<Bson> pipeline = new ArrayList<>();
    Bson userIdFilter = eq("userId", email);
    pipeline.add(userIdFilter);
    user = usersCollection
            .aggregate(pipeline)
            .first();
    //TODO> Ticket: User Management - implement the query that returns the first User object.
    return user;
  }

  /**
   * Given the userId, returns a Session object.
   *
   * @param userId - user string identifier.
   * @return Session object or null.
   */
  public Session getUserSession(String userId) {
    Session session = null;
    List<Bson> pipeline = new ArrayList<>();
    Bson sessionFilter = eq("userId", userId);
    pipeline.add(sessionFilter);
    if(!(sessionsCollection.aggregate(pipeline).first() == null)) {
      return sessionsCollection.aggregate(pipeline).first();
    }
    return null;
  }

  public boolean deleteUserSessions(String userId) {
    List<Bson> pipeline = new ArrayList<>();
    Bson sessionFilter = eq("userId", userId);
    pipeline.add(sessionFilter);
    if(!(sessionsCollection.aggregate(pipeline).first() == null)) {
      Session sessionToDelete = sessionsCollection.aggregate(pipeline).first();
      sessionsCollection.deleteOne(new Document("jwt", sessionToDelete.getJwt()));
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
    List<Bson> pipeline = new ArrayList<>();
    Bson sessionFilter = eq("email", email);
    pipeline.add(sessionFilter);
    if(!(usersCollection.aggregate(pipeline).first() == null)) {
      User userToDelete = usersCollection.aggregate(pipeline).first();
      usersCollection.deleteOne(new Document("email", userToDelete.getEmail()));
    }
    return false;
  }

  /**
   * Updates the preferences of an user identified by `email` parameter.
   *
   * @param email - user to be updated email
   * @param userPreferences - set of preferences that should be stored and replace the existing
   *     ones. Cannot be set to null value
   * @return User object that just been updated.
   */
  public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
    //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
    // be updated.
    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions when updating an entry.
    return false;
  }
}
