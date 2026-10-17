package com.pha.trainees.chemistry.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 容器端口（§19.28）：装在容器"口"所在的那一格里、决定该面通不通气的方块。
 *
 * <p>这是 #3 大型反应釜与管道系统的公共抽象第一步 —— <b>"端口提供能力，宿主聚合"</b>：
 * 容器只问"我这个面上的邻居是不是端口、它现在是开还是关"，不关心端口的具体实现
 * （阀门、将来的法兰、管道接口都实现同一个接口）。</p>
 *
 * <p><b>注意作用范围</b>：气体网格本身<b>没有渗透性模型</b>（相邻格之间一律可扩散，墙不挡气），
 * 所以端口只作用于"容器边界"这一层——它管的是"容器口开不开"，
 * 不是"气体能不能穿墙"（后者属于管道系统那一批）。</p>
 */
public interface IContainerPort {

    /**
     * 该端口此刻是否让气体通过。
     *
     * @param level           端口所在世界
     * @param portPos         端口方块自身的位置
     * @param sideToContainer 从端口指向容器的方向（由容器的面反向得到）
     * @return true = 通（该面算作容器的开口，气体在"端口自己那一格"与容器之间交换）
     */
    boolean isPortPassable(Level level, BlockPos portPos, Direction sideToContainer);
}
