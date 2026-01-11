package com.project.search_service.repository;

import com.project.search_service.domain.dto.UserSearchResponseDto;
import com.project.search_service.domain.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

@EnableElasticsearchRepositories
public interface UsersRepository extends ElasticsearchRepository<Users, String> {

  @Query("""
      {
        "bool": {
          "filter": [
            { "term": { "enable": true } }
          ],
          "should": [
            {
              "multi_match": {
                "query": "?0",
                "fields": ["username^3", "display_name^2"],
                "type": "phrase"
              }
            },
            {
              "multi_match": {
                "query": "?0",
                "fields": ["username^3", "display_name^2", "bio^1"],
                "fuzziness": "AUTO"
              }
            }
          ],
          "minimum_should_match": 1
        }
      }
      """)
  Page<UserSearchResponseDto> searchByKeyword(String keyword, Pageable pageable);

}
