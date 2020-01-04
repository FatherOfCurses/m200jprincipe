package mflix.api.utils;

import mflix.api.models.Actor;
import org.bson.*;
import org.bson.codecs.*;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

public class ActorCodec implements CollectibleCodec<Actor> {

  private final Codec<Document> documentCodec;

  public ActorCodec() {
    super();
    this.documentCodec = new DocumentCodec();
  }

  public void encode(
      BsonWriter bsonWriter, Actor actor, EncoderContext encoderContext) {

    Document actorDoc = new Document();
    ObjectId actorId = actor.getId();
    String name = actor.getName();
    Date dateOfBirth = actor.getDateOfBirth();
    List awards = actor.getAwards();
    int numMovies = actor.getNumMovies();

    if (null != actorId) {
      actorDoc.put("_id",  actorId);
    }

    if (null != name) {
      actorDoc.put("name", name);
    }

    if (null != dateOfBirth) {
      actorDoc.put("date_of_birth", dateOfBirth);
    }

    if (null != awards) {
      actorDoc.put("awards", awards);
    }

    if (0 != numMovies) {
      actorDoc.put("num_movies", numMovies);
    }

    documentCodec.encode(bsonWriter, actorDoc, encoderContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Actor decode(BsonReader bsonReader, DecoderContext decoderContext) {
    Document actorDoc = documentCodec.decode(bsonReader, decoderContext);
    Actor actor = new Actor();
    actor.setId(actorDoc.getObjectId("_id"));
    actor.setName(actorDoc.getString("name"));
    actor.setDateOfBirth(actorDoc.getDate("date_of_birth"));
    actor.setAwards((List<Document>) actorDoc.get("awards"));
    actor.setNumMovies(actorDoc.getInteger("num_movies"));
    return actor;
  }

  @Override
  public Class<Actor> getEncoderClass() {
    return Actor.class;
  }

  @Override
  public Actor generateIdIfAbsentFromDocument(Actor actor) {
    return !documentHasId(actor) ? actor.withNewId() : actor;
  }

  @Override
  public boolean documentHasId(Actor actor) {
    return null != actor.getId();
  }

  @Override
  public BsonObjectId getDocumentId(Actor actor) {
    if (!documentHasId(actor)) {
      throw new IllegalStateException("This document does not have an " + "_id");
    }

    return new BsonObjectId(actor.getId());
  }
}
