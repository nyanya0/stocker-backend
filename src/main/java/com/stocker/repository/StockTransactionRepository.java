package com.stocker.repository;

import com.stocker.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    @Query("select t from StockTransaction t join fetch t.stock " +
           "where t.user.id = :userId order by t.createdAt desc, t.id desc")
    List<StockTransaction> findHistoryForUser(@Param("userId") Long userId);

    @Query("select t from StockTransaction t left join fetch t.user join fetch t.stock " +
           "order by t.createdAt desc, t.id desc")
    List<StockTransaction> findAllWithDetails();

    @Modifying(flushAutomatically = true)
    @Query("update StockTransaction t set t.user = null where t.user.id = :userId")
    int detachUser(@Param("userId") Long userId);
}
