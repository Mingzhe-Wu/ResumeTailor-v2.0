package com.mingzhe.resumetailor.user;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis mapper for User database operations.
 */
@Mapper
public interface UserMapper {

    @Insert("""
        INSERT INTO users (
            email,
            password_hash,
            display_name
        ) VALUES (
            #{email},
            #{password},
            COALESCE(#{displayName}, 'Anonymous User')
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("""
        SELECT
            id,
            email,
            password_hash AS password,
            display_name,
            role::text AS role,
            status::text AS status,
            created_at,
            updated_at
        FROM users
        WHERE id = #{id}
        """)
    User findById(Long id);

    @Select("""
        SELECT
            id,
            email,
            password_hash AS password,
            display_name,
            role::text AS role,
            status::text AS status,
            created_at,
            updated_at
        FROM users
        WHERE email = #{email}
        """)
    User findByEmail(String email);

    @Update("""
        <script>
        UPDATE users
        <set>
            <if test="email != null">email = #{email},</if>
            <if test="password != null">password_hash = #{password},</if>
            <if test="displayName != null">display_name = #{displayName},</if>
            updated_at = NOW()
        </set>
        WHERE id = #{id}
        </script>
        """)
    int updateById(User user);

    @Delete("""
        DELETE FROM users
        WHERE id = #{id}
        """)
    int deleteById(Long id);

}
