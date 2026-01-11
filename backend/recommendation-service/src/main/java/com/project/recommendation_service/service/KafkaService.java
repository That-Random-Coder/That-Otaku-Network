package com.project.recommendation_service.service;

import com.project.recommendation_service.domain.entity.Content;
import com.project.recommendation_service.domain.entity.UsersInteraction;
import com.project.recommendation_service.domain.enums.KafkaDomain;
import com.project.recommendation_service.exception.ContentNotFoundException;
import com.project.recommendation_service.exception.InteractionNotFoundException;
import com.project.recommendation_service.repository.ContentRepository;
import com.project.recommendation_service.repository.InteractRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaService {

    private final ContentRepository contentRepository;
    private final InteractRepository interactRepository;

    @Transactional
    public void createContent(Map<String , Object> requestMap){
        UUID id = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        Object titleObj = requestMap.get(KafkaDomain.CONTENT_TITLE.toString());
        String title = (titleObj instanceof String) ? (String) titleObj : ((List<?>) titleObj).get(0).toString();

        Object usernameObj = requestMap.get(KafkaDomain.USERNAME.toString());
        String username = (usernameObj instanceof String) ? (String) usernameObj : ((List<?>) usernameObj).get(0).toString();

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        Set<String> category = new HashSet<>((List<String>) requestMap.getOrDefault(KafkaDomain.CONTENT_CATEGORY.toString(), List.of()));
        Set<String> genre = new HashSet<>((List<String>) requestMap.getOrDefault(KafkaDomain.CONTENT_GENRE.toString(), List.of()));
        Set<String> tags = new HashSet<>((List<String>) requestMap.getOrDefault(KafkaDomain.CONTENT_TAG.toString(), List.of()));


        Content content = Content
                .builder()
                .contentId(id)
                .contentTitle(title)
                .username(username)
                .timeOfCreation(dateTime)
                .likeCount(0)
                .dislikeCount(0)
                .commentCount(0)
                .shareCount(0)
                .category(category)
                .genre(genre)
                .contentTag(tags)
                .enable(true)
                .build();

        contentRepository.save(content);
        log.info("Content is saved by Id : {}", id.toString());
    }


    @Transactional
    public void deleteContent(Map<String , Object> requestMap){
        UUID id = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        Content content = contentRepository.findById(id)
                        .orElseThrow(() -> {
                            log.error("Content is not found : {}" , id.toString());
                            return new ContentNotFoundException("Content : " + id.toString());
                        });

        contentRepository.delete(content);
    }

    @Transactional
    public void disableContent(Map<String , Object> requestMap){
        UUID id = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        int update = contentRepository.updateEnableById(id , false);
        if(update == 0){
            log.error("Content is not found : {} for Disable" , id.toString());
            throw new ContentNotFoundException("Content : "+ id.toString());
        }else{
            log.info("Content with id : {} is diable" , id.toString());
        }
    }

    @Transactional
    public void enableContent(Map<String , Object> requestMap){
        UUID id = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        int update = contentRepository.updateEnableById(id , true);
        if(update == 0){
            log.error("Content is not found : {} for Enable" , id.toString());
            throw new ContentNotFoundException("Content : "+ id.toString());
        }else{
            log.info("Content with id : {} is enable" , id.toString());
        }
    }

    @Transactional
    public void likeContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> {
                    log.error("Content : {} doesn't found in Like" , contentId);
                    throw new ContentNotFoundException("Content not found with id : " + contentId.toString());
                });
        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){
            interaction = UsersInteraction
                    .builder()
                    .userId(userId)
                    .content(content)
                    .like(true)
                    .dislike(false)
                    .share(false)
                    .comment(false)
                    .interactAt(dateTime)
                    .build();

            interactRepository.save(interaction);
        }else{
            interaction.setLike(true);
            interaction.setDislike(false);

            interactRepository.save(interaction);
        }

        content.setLikeCount(content.getLikeCount() + 1);
        contentRepository.save(content);
        log.info("User : {} liked the content with Id : {}", userId ,contentId );
    }

    @Transactional
    public void dislikeContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> {
                    log.error("Content : {} doesn't found in Dislike" , contentId);
                    throw new ContentNotFoundException("Content not found with id : " + contentId.toString());
                });
        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){

            interaction = UsersInteraction
                    .builder()
                    .userId(userId)
                    .content(content)
                    .like(false)
                    .dislike(true)
                    .share(false)
                    .comment(false)
                    .interactAt(dateTime)
                    .build();

            interactRepository.save(interaction);
        }else{
            interaction.setLike(false);
            interaction.setDislike(true);

            interactRepository.save(interaction);
        }
        content.setDislikeCount(content.getDislikeCount() + 1);
        contentRepository.save(content);
        log.info("User : {} disliked the content with Id : {}", userId ,contentId );
    }

    @Transactional
    public void shareContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> {
                    log.error("Content : {} doesn't found in Share" , contentId);
                    throw new ContentNotFoundException("Content not found with id : " + contentId.toString());
                });
        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){

            interaction = UsersInteraction
                    .builder()
                    .userId(userId)
                    .content(content)
                    .like(false)
                    .dislike(false)
                    .share(true)
                    .comment(false)
                    .interactAt(dateTime)
                    .build();

            interactRepository.save(interaction);
        }else{
            interaction.setShare(true);

            interactRepository.save(interaction);
        }
        content.setShareCount(content.getShareCount() + 1);
        contentRepository.save(content);
        log.info("User : {} shared the content with Id : {}", userId ,contentId );
    }

    @Transactional
    public void commentContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> {
                    log.error("Content : {} doesn't found in comment" , contentId);
                    throw new ContentNotFoundException("Content not found with id : " + contentId.toString());
                });

        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){


            interaction = UsersInteraction
                    .builder()
                    .userId(userId)
                    .content(content)
                    .like(false)
                    .dislike(false)
                    .share(false)
                    .comment(true)
                    .interactAt(dateTime)
                    .build();

            interactRepository.save(interaction);
        }else{
            interaction.setComment(true);

            interactRepository.save(interaction);
        }

        content.setCommentCount(content.getCommentCount() + 1);
        contentRepository.save(content);
        log.info("User : {} comment the content with Id : {}", userId ,contentId );
    }


    @Transactional
    public void removeLikeContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){
            throw new InteractionNotFoundException("Interaction id Remove Like with id : " + userId);
        }else{
            interaction.setLike(false);

            interactRepository.save(interaction);
        }
        contentRepository.decreamentLikeCount(contentId);
        log.info("User : {} remove like the content with Id : {}", userId ,contentId );
    }

    @Transactional
    public void removeDisLikeContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){
            throw new InteractionNotFoundException("Interaction id Remove DisLike with id : " + userId);
        }else{
            interaction.setDislike(false);

            interactRepository.save(interaction);
        }

        contentRepository.decreamentDislikeCount(contentId);
        log.info("User : {} remove dislike the content with Id : {}", userId ,contentId );
    }

    @Transactional
    public void changeDisLikeContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){
            throw new InteractionNotFoundException("Interaction id dislike to like with id : " + userId);
        }else{
            interaction.setDislike(false);
            interaction.setLike(true);

            interactRepository.save(interaction);
            contentRepository.dislikeToLikeCount(contentId);
        }
        log.info("User : {} remove dislike to like the content with Id : {}", userId ,contentId );
    }

    @Transactional
    public void changeLikeContent(Map<String , Object> requestMap){
        UUID contentId = UUID.fromString((String) requestMap.get(KafkaDomain.CONTENT_ID.toString()));
        UUID userId = UUID.fromString((String) requestMap.get(KafkaDomain.USER_ID.toString()));

        List<Integer> timeList = (List<Integer>) requestMap.get(KafkaDomain.TIME_OF_CREATION.toString());
        LocalDateTime dateTime = LocalDateTime.of(
                timeList.get(0), timeList.get(1), timeList.get(2),
                timeList.get(3), timeList.get(4), timeList.get(5), timeList.get(6)
        );

        UsersInteraction interaction = interactRepository.findByUserIdAndContent(userId , contentId);

        if(interaction == null){
            throw new InteractionNotFoundException("Interaction id like to dislike with id : " + userId);
        }else{
            interaction.setDislike(true);
            interaction.setLike(false);

            interactRepository.save(interaction);
            contentRepository.likeToDisLikeCount(contentId);
        }
        log.info("User : {} remove like to dislike the content with Id : {}", userId ,contentId );
    }


}
