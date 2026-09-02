package org.egovframe.cloud.boardservice.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.egovframe.cloud.boardservice.domain.board.Board;
import org.egovframe.cloud.boardservice.domain.board.BoardRepository;
import org.egovframe.cloud.boardservice.domain.posts.Posts;
import org.egovframe.cloud.boardservice.domain.posts.PostsId;
import org.egovframe.cloud.boardservice.domain.posts.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * org.egovframe.cloud.boardservice.domain.comment.CommentRepositoryScopeTest
 * <p>
 * 댓글 정렬 순서 일괄 수정이 게시물 범위를 벗어나지 않는지 확인한다.
 *
 * @author 표준프레임워크센터
 * @version 1.0
 * @since 2026/08/28
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2026/08/28    contributors  최초 생성
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties
@ActiveProfiles(profiles = "test")
@Transactional
class CommentRepositoryScopeTest {

    private static final Integer POSTS_ONE = 901;
    private static final Integer POSTS_TWO = 902;
    private static final Integer GROUP_NO = 1;

    @Autowired
    private EntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private BoardRepository boardRepository;

    private Integer boardNo;

    private void givenCommentsOn(Integer postsNo) {
        PostsId postsId = PostsId.builder().boardNo(boardNo).postsNo(postsNo).build();
        Posts posts = postsRepository.save(Posts.builder()
                .board(boardRepository.findById(boardNo).orElseThrow())
                .postsId(postsId)
                .postsTitle("게시물 " + postsNo)
                .postsContent("본문")
                .readCount(0)
                .noticeAt(false)
                .deleteAt(0)
                .build());

        for (int commentNo = 1; commentNo <= 2; commentNo++) {
            commentRepository.save(Comment.builder()
                    .posts(posts)
                    .commentId(CommentId.builder().postsId(postsId).commentNo(commentNo).build())
                    .commentContent("댓글 " + commentNo)
                    .groupNo(GROUP_NO)
                    .depthSeq(0)
                    .sortSeq(commentNo)
                    .deleteAt(0)
                    .build());
        }
    }

    private List<Integer> sortSeqOf(Integer postsNo) {
        return commentRepository.findAll().stream()
                .filter(comment -> boardNo.equals(comment.getCommentId().getPostsId().getBoardNo()))
                .filter(comment -> postsNo.equals(comment.getCommentId().getPostsId().getPostsNo()))
                .sorted((a, b) -> a.getCommentId().getCommentNo() - b.getCommentId().getCommentNo())
                .map(Comment::getSortSeq)
                .toList();
    }

    @BeforeEach
    void setUp() {
        boardNo = boardRepository.save(Board.builder()
                .boardName("테스트 게시판")
                .skinTypeCode("normal")
                .titleDisplayLength(50)
                .postDisplayCount(10)
                .pageDisplayCount(10)
                .newDisplayDayCount(3)
                .editorUseAt(false)
                .userWriteAt(true)
                .commentUseAt(true)
                .uploadUseAt(false)
                .uploadLimitCount(0)
                .uploadLimitSize(BigDecimal.ZERO)
                .build()).getBoardNo();
        givenCommentsOn(POSTS_ONE);
        givenCommentsOn(POSTS_TWO);
    }

    @DisplayName("정렬 순서 일괄 수정은 지정한 게시물의 댓글만 바꾼다")
    @Test
    void updateSortSeq_only_touches_given_posts() {
        em.flush();
        em.clear();

        commentRepository.updateSortSeq(boardNo, POSTS_ONE, GROUP_NO, 1, null, 1);

        em.clear();

        assertThat(sortSeqOf(POSTS_ONE)).containsExactly(2, 3);
        assertThat(sortSeqOf(POSTS_TWO))
                .as("다른 게시물의 댓글은 그대로여야 한다")
                .containsExactly(1, 2);
    }
}
