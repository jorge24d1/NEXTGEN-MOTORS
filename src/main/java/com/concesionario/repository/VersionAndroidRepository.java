package com.concesionario.repository;

import com.concesionario.model.VersionAndroid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface VersionAndroidRepository extends MongoRepository<VersionAndroid, Integer> {
    Optional<VersionAndroid> findFirstByPublicadaNotNullOrderByCodigoDesc();
    List<VersionAndroid> findTop30ByOrderByCodigoDesc();

    @Query("{'_id': ?0, 'publicada': null}")
    @Update("{'$set': {'publicada': ?1}}")
    long publicarBorrador(int codigo, Instant fecha);
}
