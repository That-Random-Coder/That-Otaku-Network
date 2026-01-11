package com.project.search_service.repository;

import com.project.search_service.domain.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import com.project.search_service.domain.dto.GroupSearchResponseDto;
import java.util.UUID;

@EnableElasticsearchRepositories
public interface GroupRepository extends ElasticsearchRepository<Group, String> {

	Group getByIdAndEnableTrue(String id);

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
								"fields": ["groupName^4", "leader_display_name^1"],
								"type": "phrase"
							}
						},
						{
							"multi_match": {
								"query": "?0",
								"fields": ["groupName^4", "leader_display_name^1", "bio^3", "leader_username^1"],
								"fuzziness": "AUTO"
							}
						}
					],
					"minimum_should_match": 1
				}
			}
			""")
	Page<GroupSearchResponseDto> searchByKeyword(String keyword, Pageable pageable);
}
